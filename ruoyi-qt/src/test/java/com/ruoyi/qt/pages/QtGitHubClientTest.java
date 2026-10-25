package com.ruoyi.qt.pages;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.ruoyi.qt.config.QtPagesProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QtGitHubClientTest
{
    private static final String SHA_A = "a".repeat(40);

    @TempDir
    Path tempDir;

    private QtPagesProperties properties;

    @BeforeEach
    void setUp()
    {
        properties = new QtPagesProperties();
        properties.setGithubToken("ghp_test_token");
    }

    @Test
    void downloadsPinnedShaAfterBranchMoves()
    throws Exception
    {
        FakeTransport transport = new FakeTransport();
        transport.queue(json(200, "{\"private\":false,\"name\":\"hello\"}"));
        transport.queue(withEtag(json(200, "{\"ref\":\"refs/heads/pages\",\"object\":{\"sha\":\"" + SHA_A + "\"}}"), "W/\"etag-1\""));
        transport.queue(redirect("https://codeload.github.com/octo/hello/tar.gz/" + SHA_A));
        transport.queue(raw(200, Map.of(), new ByteArrayInputStream(new byte[] { 0x1f, 0x00 })));

        QtGitHubClient client = new QtGitHubClient(properties, transport);
        QtGitHubClient.Source source = new QtGitHubClient.Source("octo", "hello", "pages");
        client.requirePublicRepository(source);
        QtGitHubClient.Head head = client.getBranchHead(source, null);
        assertEquals(SHA_A, head.sha());
        assertEquals("W/\"etag-1\"", head.etag());

        Path destination = tempDir.resolve("archive.tgz");
        client.downloadArchive(source, head.sha(), destination);

        assertEquals("https://api.github.com/repos/octo/hello/tarball/" + SHA_A,
                transport.requests.get(2).uri().toString());
        assertEquals("https://codeload.github.com/octo/hello/tar.gz/" + SHA_A,
                transport.requests.get(3).uri().toString());
        assertTrue(Files.exists(destination));
        assertEquals(2, Files.size(destination));
    }

    @Test
    void encodesSlashBranch()
    {
        FakeTransport transport = new FakeTransport();
        transport.queue(json(200, "{\"ref\":\"refs/heads/feature/one\",\"object\":{\"sha\":\"" + SHA_A + "\"}}"));

        QtGitHubClient client = new QtGitHubClient(properties, transport);
        client.getBranchHead(new QtGitHubClient.Source("octo", "hello", "feature/one"), null);

        assertTrue(transport.requests.get(0).uri().toString().endsWith("/git/refs/heads/feature%2Fone"));
    }

    @Test
    void rejectsPrivateRepoEvenWithToken()
    {
        FakeTransport transport = new FakeTransport();
        transport.queue(json(200, "{\"private\":true}"));

        QtGitHubClient client = new QtGitHubClient(properties, transport);
        QtGitHubClient.GitHubClientException exception = assertThrows(QtGitHubClient.GitHubClientException.class,
                () -> client.requirePublicRepository(new QtGitHubClient.Source("octo", "hello", "pages")));
        assertEquals("PRIVATE_REPO", exception.getErrorCode());
        assertFalse(exception.isRetryable());
        assertEquals(1, transport.requests.size());
    }

    @Test
    void notModifiedHeadHasNoSha()
    {
        FakeTransport transport = new FakeTransport();
        transport.queue(withEtag(raw(304, Map.of(), InputStream.nullInputStream()), "W/\"etag-1\""));

        QtGitHubClient client = new QtGitHubClient(properties, transport);
        QtGitHubClient.Head head = client.getBranchHead(new QtGitHubClient.Source("octo", "hello", "pages"), "W/\"etag-1\"");

        assertTrue(head.notModified());
        assertNull(head.sha());
        assertTrue(transport.requests.get(0).headers().containsKey("If-None-Match"));
    }

    @Test
    void missingBranchSurfacesStableError()
    {
        FakeTransport transport = new FakeTransport();
        transport.queue(json(404, "{\"message\":\"Not Found\"}"));

        QtGitHubClient client = new QtGitHubClient(properties, transport);
        QtGitHubClient.GitHubClientException exception = assertThrows(QtGitHubClient.GitHubClientException.class,
                () -> client.getBranchHead(new QtGitHubClient.Source("octo", "hello", "missing"), null));
        assertEquals("BRANCH_NOT_FOUND", exception.getErrorCode());
        assertFalse(exception.isRetryable());
    }

    @Test
    void rejectsRedirectToPrivateOrUnknownHost()
    throws Exception
    {
        for (String location : List.of("http://127.0.0.1/x", "https://evil.example/x", "https://user@github.com/x"))
        {
            FakeTransport transport = new FakeTransport();
            transport.queue(redirect(location));
            QtGitHubClient client = new QtGitHubClient(properties, transport);
            assertThrows(QtGitHubClient.GitHubClientException.class,
                    () -> client.downloadArchive(new QtGitHubClient.Source("octo", "hello", "pages"), SHA_A,
                            tempDir.resolve("bad.tgz")));
            assertEquals(1, transport.requests.size(), "redirect target must not be requested: " + location);
        }
    }

    @Test
    void doesNotForwardTokenToCodeload()
    throws Exception
    {
        FakeTransport transport = new FakeTransport();
        transport.queue(json(200, "{\"private\":false}"));
        transport.queue(redirect("https://codeload.github.com/octo/hello/tar.gz/" + SHA_A));
        transport.queue(raw(200, Map.of(), new ByteArrayInputStream(new byte[] { 0x1f })));

        QtGitHubClient client = new QtGitHubClient(properties, transport);
        QtGitHubClient.Source source = new QtGitHubClient.Source("octo", "hello", "pages");
        client.requirePublicRepository(source);
        client.downloadArchive(source, SHA_A, tempDir.resolve("a.tgz"));

        assertNotNull(transport.requests.get(0).headers().get("Authorization"));
        assertFalse(transport.requests.get(2).headers().containsKey("Authorization"));
    }

    @Test
    void allowsAtMostThreeRedirectHops()
    {
        FakeTransport transport = new FakeTransport();
        transport.queue(redirect("https://codeload.github.com/octo/hello/tar.gz/1"));
        transport.queue(redirect("https://codeload.github.com/octo/hello/tar.gz/2"));
        transport.queue(redirect("https://codeload.github.com/octo/hello/tar.gz/3"));
        transport.queue(redirect("https://codeload.github.com/octo/hello/tar.gz/4"));

        QtGitHubClient client = new QtGitHubClient(properties, transport);
        QtGitHubClient.GitHubClientException exception = assertThrows(QtGitHubClient.GitHubClientException.class,
                () -> client.downloadArchive(new QtGitHubClient.Source("octo", "hello", "pages"), SHA_A,
                        tempDir.resolve("loop.tgz")));
        assertEquals("REDIRECT_REJECTED", exception.getErrorCode());
    }

    @Test
    void streamEnforcesDownloadLimitAndCleansUp()
    throws Exception
    {
        properties.setDownloadLimitBytes(1000);
        FakeTransport transport = new FakeTransport();
        FakeInputStream body = new FakeInputStream(4096);
        transport.queue(raw(200, Map.of(), body));

        QtGitHubClient client = new QtGitHubClient(properties, transport);
        Path destination = tempDir.resolve("huge.tgz");
        QtGitHubClient.GitHubClientException exception = assertThrows(QtGitHubClient.GitHubClientException.class,
                () -> client.downloadArchive(new QtGitHubClient.Source("octo", "hello", "pages"), SHA_A, destination));
        assertEquals("ARCHIVE_TOO_LARGE", exception.getErrorCode());
        assertFalse(Files.exists(destination));
        assertFalse(Files.exists(destination.resolveSibling("huge.tgz.part")));
        assertTrue(body.closed);
    }

    @Test
    void contentLengthAboveLimitFailsFastWithoutReading()
    throws Exception
    {
        properties.setDownloadLimitBytes(1000);
        FakeTransport transport = new FakeTransport();
        FakeInputStream body = new FakeInputStream(0);
        transport.queue(raw(200, Map.of("Content-Length", List.of("5000")), body));

        QtGitHubClient client = new QtGitHubClient(properties, transport);
        QtGitHubClient.GitHubClientException exception = assertThrows(QtGitHubClient.GitHubClientException.class,
                () -> client.downloadArchive(new QtGitHubClient.Source("octo", "hello", "pages"), SHA_A,
                        tempDir.resolve("cl.tgz")));
        assertEquals("ARCHIVE_TOO_LARGE", exception.getErrorCode());
        assertTrue(body.closed);
        assertEquals(0, body.readBytes);
    }

    @Test
    void honorsRetryAfterHeader()
    {
        FakeTransport transport = new FakeTransport();
        transport.queue(raw(403, Map.of("Retry-After", List.of("60")), InputStream.nullInputStream()));

        QtGitHubClient client = new QtGitHubClient(properties, transport);
        QtGitHubClient.GitHubClientException exception = assertThrows(QtGitHubClient.GitHubClientException.class,
                () -> client.requirePublicRepository(new QtGitHubClient.Source("octo", "hello", "pages")));
        assertEquals("RATE_LIMITED", exception.getErrorCode());
        assertTrue(exception.isRetryable());
        assertNotNull(exception.getRetryAt());
        long seconds = exception.getRetryAt().getEpochSecond() - Instant.now().getEpochSecond();
        assertTrue(seconds >= 50 && seconds <= 70, "retryAt should be ~60s away: " + seconds);
    }

    @Test
    void honorsRateLimitResetHeader()
    {
        FakeTransport transport = new FakeTransport();
        long resetEpoch = Instant.now().plusSeconds(120).getEpochSecond();
        transport.queue(raw(429, Map.of("X-RateLimit-Reset", List.of(String.valueOf(resetEpoch))), InputStream.nullInputStream()));

        QtGitHubClient client = new QtGitHubClient(properties, transport);
        QtGitHubClient.GitHubClientException exception = assertThrows(QtGitHubClient.GitHubClientException.class,
                () -> client.getBranchHead(new QtGitHubClient.Source("octo", "hello", "pages"), null));
        assertEquals("RATE_LIMITED", exception.getErrorCode());
        assertNotNull(exception.getRetryAt());
        assertEquals(resetEpoch, exception.getRetryAt().getEpochSecond());
    }

    @Test
    void upstreamServerErrorIsRetryable()
    {
        FakeTransport transport = new FakeTransport();
        transport.queue(json(502, "{}"));

        QtGitHubClient client = new QtGitHubClient(properties, transport);
        QtGitHubClient.GitHubClientException exception = assertThrows(QtGitHubClient.GitHubClientException.class,
                () -> client.requirePublicRepository(new QtGitHubClient.Source("octo", "hello", "pages")));
        assertEquals("UPSTREAM_ERROR", exception.getErrorCode());
        assertTrue(exception.isRetryable());
    }

    private static FakeTransport.Response json(int status, String body)
    {
        return raw(status, Map.of(), new ByteArrayInputStream(body.getBytes()));
    }

    private static FakeTransport.Response withEtag(FakeTransport.Response response, String etag)
    {
        response.headers.put("ETag", List.of(etag));
        return response;
    }

    private static FakeTransport.Response redirect(String location)
    {
        return raw(302, Map.of("Location", List.of(location)), InputStream.nullInputStream());
    }

    private static FakeTransport.Response raw(int status, Map<String, List<String>> headers, InputStream body)
    {
        return new FakeTransport.Response(status, new HashMap<>(headers), body);
    }

    /** 记录请求并按序返回预置响应；收到非 api.github.com/codeload.github.com 的请求直接失败。 */
    static class FakeTransport implements QtGitHubClient.Transport
    {
        final List<Request> requests = new ArrayList<>();
        private final List<Response> responses = new ArrayList<>();

        void queue(Response response)
        {
            responses.add(response);
        }

        @Override
        public Response exchange(Request request)
        {
            requests.add(request);
            String host = request.uri().getHost();
            if (!"api.github.com".equals(host) && !"codeload.github.com".equals(host))
            {
                throw new IllegalStateException("client must not contact host: " + host);
            }
            if (responses.isEmpty())
            {
                throw new IllegalStateException("no canned response for " + request.uri());
            }
            return responses.remove(0);
        }

        static class Response implements QtGitHubClient.Transport.Response
        {
            private final int status;
            private final Map<String, List<String>> headers;
            private final InputStream body;

            Response(int status, Map<String, List<String>> headers, InputStream body)
            {
                this.status = status;
                this.headers = headers;
                this.body = body;
            }

            @Override
            public int status()
            {
                return status;
            }

            @Override
            public Map<String, List<String>> headers()
            {
                return headers;
            }

            @Override
            public InputStream body()
            {
                return body;
            }
        }
    }

    static class FakeInputStream extends InputStream
    {
        private final byte[] payload = new byte[512];
        private int remaining;
        boolean closed;
        int readBytes;

        FakeInputStream(int totalBytes)
        {
            remaining = totalBytes;
        }

        @Override
        public int read()
        {
            return read(new byte[1], 0, 1);
        }

        @Override
        public int read(byte[] buffer, int offset, int length)
        {
            if (remaining <= 0)
            {
                return -1;
            }
            int chunk = Math.min(length, Math.min(remaining, payload.length));
            remaining -= chunk;
            readBytes += chunk;
            return chunk;
        }

        @Override
        public void close()
        {
            closed = true;
        }
    }
}
