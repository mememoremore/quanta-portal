package com.ruoyi.qt.pages;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.qt.config.QtPagesProperties;

/**
 * GitHub 公开仓库只读客户端。
 *
 * 只接受固定主机（api.github.com / codeload.github.com）；仓库必须 private=false，
 * 即使平台 Token 误带私有权限也不发布私有内容。下载固定使用分支头解析出的 SHA，
 * 手动跟随重定向且不向下载主机转发 Authorization。错误统一转换为带稳定 errorCode、
 * retryable 标记与可选 retryAt 的内部异常，不把上游响应正文或本机路径暴露给成员。
 */
public class QtGitHubClient
{
    public static final String ERROR_REPO_NOT_FOUND = "REPO_NOT_FOUND";

    public static final String ERROR_BRANCH_NOT_FOUND = "BRANCH_NOT_FOUND";

    public static final String ERROR_PRIVATE_REPO = "PRIVATE_REPO";

    public static final String ERROR_RATE_LIMITED = "RATE_LIMITED";

    public static final String ERROR_UPSTREAM = "UPSTREAM_ERROR";

    public static final String ERROR_BAD_RESPONSE = "BAD_RESPONSE";

    public static final String ERROR_NETWORK = "NETWORK";

    public static final String ERROR_TIMEOUT = "TIMEOUT";

    public static final String ERROR_TOO_LARGE = "ARCHIVE_TOO_LARGE";

    public static final String ERROR_REDIRECT_REJECTED = "REDIRECT_REJECTED";

    public static final String ERROR_DOWNLOAD_FAILED = "DOWNLOAD_FAILED";

    private static final String API_HOST = "api.github.com";

    private static final String DOWNLOAD_HOST = "codeload.github.com";

    private static final int MAX_REDIRECT_HOPS = 3;

    private static final Pattern SHA_PATTERN = Pattern.compile("^[0-9a-fA-F]{40,64}$");

    private static final String USER_AGENT = "QuantaCenter-Pages";

    public record Source(String owner, String repo, String branch)
    {
    }

    public record Head(String sha, String etag, boolean notModified)
    {
    }

    /**
     * 单次 HTTP 交换抽象：手动重定向、无自动跟随，便于测试注入假传输层。
     */
    public interface Transport
    {
        record Request(URI uri, Map<String, List<String>> headers)
        {
        }

        interface Response extends AutoCloseable
        {
            int status();

            Map<String, List<String>> headers();

            InputStream body();

            @Override
            default void close()
            {
                try
                {
                    body().close();
                }
                catch (IOException ignored)
                {
                }
            }
        }

        Response exchange(Request request) throws IOException;
    }

    /**
     * 带 errorCode / retryable / retryAt 的内部异常；message 仅用于日志，不直接展示成员。
     */
    public static class GitHubClientException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;

        private final String errorCode;

        private final boolean retryable;

        private final Instant retryAt;

        public GitHubClientException(String errorCode, boolean retryable, String message)
        {
            this(errorCode, retryable, null, message);
        }

        public GitHubClientException(String errorCode, boolean retryable, Instant retryAt, String message)
        {
            super(message);
            this.errorCode = errorCode;
            this.retryable = retryable;
            this.retryAt = retryAt;
        }

        public String getErrorCode()
        {
            return errorCode;
        }

        public boolean isRetryable()
        {
            return retryable;
        }

        public Instant getRetryAt()
        {
            return retryAt;
        }
    }

    private final QtPagesProperties properties;

    private final Transport transport;

    public QtGitHubClient(QtPagesProperties properties)
    {
        this(properties, new HttpTransportAdapter(properties));
    }

    QtGitHubClient(QtPagesProperties properties, Transport transport)
    {
        this.properties = properties;
        this.transport = transport;
    }

    /**
     * 确认仓库存在且公开；私有仓库一律拒绝。
     */
    public void requirePublicRepository(Source source)
    {
        URI uri = apiUri("/repos/" + source.owner() + "/" + source.repo());
        try (Transport.Response response = exchangeApi(uri))
        {
            if (response.status() == 200)
            {
                JSONObject json = readJson(response);
                if (Boolean.TRUE.equals(json.getBoolean("private")))
                {
                    throw new GitHubClientException(ERROR_PRIVATE_REPO, false, "repository is private");
                }
                return;
            }
            throw mapFailure(response.status(), response.headers(), ERROR_REPO_NOT_FOUND);
        }
    }

    /**
     * 解析分支头。带 ETag 时返回 304 则不产生部署；SHA 只认 40-64 位十六进制。
     */
    public Head getBranchHead(Source source, String etag)
    {
        String encodedBranch = URLEncoder.encode(source.branch(), StandardCharsets.UTF_8);
        URI uri = apiUri("/repos/" + source.owner() + "/" + source.repo() + "/git/refs/heads/" + encodedBranch);
        try (Transport.Response response = exchangeApi(uri, etag))
        {
            if (response.status() == 304)
            {
                return new Head(null, etag, true);
            }
            if (response.status() == 200)
            {
                JSONObject json = readJson(response);
                JSONObject object = json.getJSONObject("object");
                String sha = object == null ? null : object.getString("sha");
                if (sha == null || !SHA_PATTERN.matcher(sha).matches())
                {
                    throw new GitHubClientException(ERROR_BAD_RESPONSE, false, "branch head response missing sha");
                }
                String responseEtag = firstHeader(response.headers(), "ETag");
                return new Head(sha, responseEtag != null ? responseEtag : etag, false);
            }
            throw mapFailure(response.status(), response.headers(), ERROR_BRANCH_NOT_FOUND);
        }
    }

    /**
     * 下载指定 SHA 的 tar.gz 到 destination。跟随重定向仅允许 https://codeload.github.com，
     * 最多 3 跳；流式读取强制下载上限，超限或失败时删除未完成文件。
     */
    public void downloadArchive(Source source, String sha, Path destination)
    {
        if (sha == null || !SHA_PATTERN.matcher(sha).matches())
        {
            throw new IllegalArgumentException("invalid commit sha");
        }
        URI current = apiUri("/repos/" + source.owner() + "/" + source.repo() + "/tarball/" + sha);
        long deadlineNanos = System.nanoTime() + Duration.ofSeconds(properties.getDownloadTimeoutSeconds()).toNanos();
        for (int hop = 0; ; hop++)
        {
            Transport.Response response;
            try
            {
                response = transport.exchange(new Transport.Request(current, downloadHeaders()));
            }
            catch (IOException e)
            {
                throw new GitHubClientException(ERROR_NETWORK, true, "download request failed: " + e.getMessage());
            }
            try
            {
                int status = response.status();
                if (status == 301 || status == 302 || status == 307 || status == 308)
                {
                    if (hop >= MAX_REDIRECT_HOPS)
                    {
                        throw new GitHubClientException(ERROR_REDIRECT_REJECTED, false, "too many redirects");
                    }
                    String location = firstHeader(response.headers(), "Location");
                    current = validateDownloadTarget(location);
                    continue;
                }
                if (status == 200)
                {
                    String declaredLength = firstHeader(response.headers(), "Content-Length");
                    if (declaredLength != null)
                    {
                        try
                        {
                            if (Long.parseLong(declaredLength.trim()) > properties.getDownloadLimitBytes())
                            {
                                throw new GitHubClientException(ERROR_TOO_LARGE, false, "archive exceeds download limit");
                            }
                        }
                        catch (NumberFormatException ignored)
                        {
                        }
                    }
                    copyWithLimits(response.body(), destination,
                            properties.getDownloadLimitBytes(), deadlineNanos);
                    return;
                }
                throw mapFailure(status, response.headers(), ERROR_DOWNLOAD_FAILED);
            }
            finally
            {
                response.close();
            }
        }
    }

    private URI validateDownloadTarget(String location)
    {
        if (location == null || location.isBlank())
        {
            throw new GitHubClientException(ERROR_REDIRECT_REJECTED, false, "redirect without location");
        }
        URI next;
        try
        {
            next = URI.create(location.trim());
        }
        catch (IllegalArgumentException e)
        {
            throw new GitHubClientException(ERROR_REDIRECT_REJECTED, false, "invalid redirect location");
        }
        String scheme = next.getScheme() == null ? "" : next.getScheme().toLowerCase(Locale.ROOT);
        String host = next.getHost() == null ? "" : next.getHost().toLowerCase(Locale.ROOT);
        int port = next.getPort();
        if (!"https".equals(scheme) || !DOWNLOAD_HOST.equals(host)
                || (port != -1 && port != 443) || next.getUserInfo() != null
                || next.getQuery() != null || next.getFragment() != null)
        {
            throw new GitHubClientException(ERROR_REDIRECT_REJECTED, false, "redirect target not allowed: " + host);
        }
        return next;
    }

    private void copyWithLimits(InputStream body, Path destination, long limit, long deadlineNanos)
    {
        Path parent = destination.toAbsolutePath().getParent();
        Path temp = destination.toAbsolutePath().resolveSibling(destination.getFileName() + ".part");
        long total = 0;
        try (InputStream source = body)
        {
            Files.createDirectories(parent);
            try (var out = Files.newOutputStream(temp, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE))
            {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = source.read(buffer)) != -1)
                {
                    total += read;
                    if (total > limit)
                    {
                        throw new GitHubClientException(ERROR_TOO_LARGE, false, "archive exceeds download limit");
                    }
                    if (System.nanoTime() > deadlineNanos)
                    {
                        throw new GitHubClientException(ERROR_TIMEOUT, true, "download exceeded time limit");
                    }
                    out.write(buffer, 0, read);
                }
            }
            try
            {
                Files.move(temp, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (IOException e)
            {
                Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (GitHubClientException e)
        {
            deleteQuietly(temp);
            throw e;
        }
        catch (IOException e)
        {
            deleteQuietly(temp);
            throw new GitHubClientException(ERROR_NETWORK, true, "download failed: " + e.getMessage());
        }
    }

    private Transport.Response exchangeApi(URI uri)
    {
        return exchangeApi(uri, null);
    }

    private Transport.Response exchangeApi(URI uri, String etag)
    {
        Map<String, List<String>> headers = apiHeaders(etag);
        try
        {
            return transport.exchange(new Transport.Request(uri, headers));
        }
        catch (IOException e)
        {
            throw new GitHubClientException(ERROR_NETWORK, true, "api request failed: " + e.getMessage());
        }
    }

    private Map<String, List<String>> apiHeaders(String etag)
    {
        Map<String, List<String>> headers = new java.util.HashMap<>();
        headers.put("Accept", List.of("application/vnd.github+json"));
        headers.put("X-GitHub-Api-Version", List.of("2022-11-28"));
        if (properties.getGithubToken() != null && !properties.getGithubToken().isBlank())
        {
            headers.put("Authorization", List.of("Bearer " + properties.getGithubToken()));
        }
        if (etag != null && !etag.isBlank())
        {
            headers.put("If-None-Match", List.of(etag));
        }
        return headers;
    }

    private Map<String, List<String>> downloadHeaders()
    {
        // 刻意不带 Authorization：下载重定向到 codeload，凭据不得离开 API 主机
        Map<String, List<String>> headers = new java.util.HashMap<>();
        headers.put("Accept", List.of("application/vnd.github+json"));
        return headers;
    }

    private URI apiUri(String path)
    {
        URI uri = URI.create("https://" + API_HOST + path);
        if (uri.getHost() == null || uri.getUserInfo() != null)
        {
            throw new IllegalArgumentException("bad api path");
        }
        return uri;
    }

    private GitHubClientException mapFailure(int status, Map<String, List<String>> headers, String notFoundCode)
    {
        if (status == 404)
        {
            return new GitHubClientException(notFoundCode, false, "github resource not found");
        }
        if (status == 403 || status == 429)
        {
            return new GitHubClientException(ERROR_RATE_LIMITED, true, retryAtFrom(headers), "github rate limited");
        }
        if (status >= 500)
        {
            return new GitHubClientException(ERROR_UPSTREAM, true, "github upstream error: " + status);
        }
        return new GitHubClientException(ERROR_BAD_RESPONSE, false, "unexpected github status: " + status);
    }

    private Instant retryAtFrom(Map<String, List<String>> headers)
    {
        String retryAfter = firstHeader(headers, "Retry-After");
        if (retryAfter != null)
        {
            try
            {
                return Instant.now().plusSeconds(Long.parseLong(retryAfter.trim()));
            }
            catch (NumberFormatException ignored)
            {
            }
        }
        String reset = firstHeader(headers, "X-RateLimit-Reset");
        if (reset != null)
        {
            try
            {
                return Instant.ofEpochSecond(Long.parseLong(reset.trim()));
            }
            catch (NumberFormatException ignored)
            {
            }
        }
        return null;
    }

    private static JSONObject readJson(Transport.Response response)
    {
        try
        {
            return JSON.parseObject(new String(response.body().readAllBytes(), StandardCharsets.UTF_8));
        }
        catch (IOException e)
        {
            throw new GitHubClientException(ERROR_BAD_RESPONSE, false, "unreadable github response");
        }
    }

    private static String firstHeader(Map<String, List<String>> headers, String name)
    {
        if (headers == null)
        {
            return null;
        }
        for (Map.Entry<String, List<String>> entry : headers.entrySet())
        {
            if (entry.getKey() != null && name.equalsIgnoreCase(entry.getKey())
                    && entry.getValue() != null && !entry.getValue().isEmpty())
            {
                return entry.getValue().get(0);
            }
        }
        return null;
    }

    private static void deleteQuietly(Path path)
    {
        try
        {
            Files.deleteIfExists(path);
        }
        catch (IOException ignored)
        {
        }
    }

    /**
     * 生产传输层：手动重定向、连接超时与请求超时按配置；发起请求前解析主机并拒绝
     * 环回/链路本地/站点本地/ULA/多播等非公网地址（SSRF 防护）。
     */
    private static class HttpTransportAdapter implements Transport
    {
        private final HttpClient client;

        private final Duration requestTimeout;

        HttpTransportAdapter(QtPagesProperties properties)
        {
            this.client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()))
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();
            this.requestTimeout = Duration.ofSeconds(properties.getDownloadTimeoutSeconds());
        }

        @Override
        public Response exchange(Request request) throws IOException
        {
            validateResolvedHostsArePublic(request.uri());
            HttpRequest.Builder builder = HttpRequest.newBuilder(request.uri()).timeout(requestTimeout).GET();
            request.headers().forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
            HttpResponse<InputStream> response;
            try
            {
                response = client.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
            }
            catch (HttpTimeoutException e)
            {
                throw new IOException("request timed out", e);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new IOException("request interrupted", e);
            }
            return new AdapterResponse(response.statusCode(), response.headers().map(), response.body());
        }

        private static void validateResolvedHostsArePublic(URI uri) throws IOException
        {
            String host = uri.getHost();
            if (host == null)
            {
                throw new IOException("request without host");
            }
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0)
            {
                throw new IOException("cannot resolve host: " + host);
            }
            for (InetAddress address : addresses)
            {
                if (address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                        || address.isAnyLocalAddress() || address.isMulticastAddress() || isUniqueLocalIpv6(address))
                {
                    throw new IOException("refusing to contact non-public address for " + host);
                }
            }
        }

        private static boolean isUniqueLocalIpv6(InetAddress address)
        {
            byte[] bytes = address.getAddress();
            return bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
        }

        private record AdapterResponse(int status, Map<String, List<String>> headers, InputStream body) implements Response
        {
            @Override
            public void close()
            {
                Transport.Response.super.close();
            }
        }
    }
}
