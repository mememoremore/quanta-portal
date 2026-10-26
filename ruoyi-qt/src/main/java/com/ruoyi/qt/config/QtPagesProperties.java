package com.ruoyi.qt.config;

import java.util.List;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.StringUtils;

/**
 * Quanta Pages（成员静态站点托管）配置，绑定 qt.pages.*。
 *
 * 功能默认关闭；只有 enabled=true 时才允许触碰磁盘目录与 GitHub 网络。
 * githubToken 只从服务器环境变量注入，不出现在 toString 与任何日志中。
 */
@Component
@ConfigurationProperties(prefix = "qt.pages")
public class QtPagesProperties
{
    /** 功能总开关，默认关闭 */
    private boolean enabled = false;

    /** 成员站点域名后缀，slug 拼接为 {slug}.{baseDomain} */
    private String baseDomain = "page.quantacenter.com";

    /** 开放 API 中头像等公开资源的绝对 URL 前缀（个人站点与门户不同源） */
    private String assetBaseUrl = "https://www.quantacenter.com";

    /** Pages 发布根目录（Linux 绝对路径），独立于 ruoyi.profile */
    private String rootDir = "/srv/quanta-pages";

    /** 服务端 GitHub Token，只用于提升 API 配额；留空时按匿名额度运行 */
    private String githubToken = "";

    /** 分支轮询间隔（秒） */
    private int pollIntervalSeconds = 300;

    /** 部署任务扫描间隔（秒） */
    private int scanIntervalSeconds = 5;

    private int pollExecutorThreads = 2;

    private int deployExecutorThreads = 2;

    private int executorQueueSize = 8;

    /** 归档下载上限（字节） */
    private long downloadLimitBytes = 100L * 1024 * 1024;

    /** 整份归档展开数据上限（字节），覆盖发布目录之外的内容 */
    private long extractedLimitBytes = 200L * 1024 * 1024;

    /** 普通文件数量上限 */
    private int maxFiles = 10_000;

    /** 归档条目总数上限 */
    private int maxEntries = 20_000;

    /** 单文件上限（字节） */
    private long maxFileBytes = 20L * 1024 * 1024;

    private int connectTimeoutSeconds = 10;

    private int downloadTimeoutSeconds = 180;

    private int deployTimeoutSeconds = 300;

    /** 可重试错误的最大尝试次数 */
    private int maxAttempts = 3;

    /** 重试退避间隔（秒），第 n 次失败后取第 n 项 */
    private List<Integer> retryBackoffSeconds = List.of(30, 120);

    /** 每站保留的最近成功实体版本数量（不含受保护引用） */
    private int retainReleases = 5;

    /** 每站磁盘占用上限（字节，含临时文件） */
    private long diskQuotaBytes = 2L * 1024 * 1024 * 1024;

    /** 阶段日志上限（字节），应用写入时截断 */
    private int logLimitBytes = 32 * 1024;

    @PostConstruct
    public void validate()
    {
        if (!enabled)
        {
            return;
        }
        if (StringUtils.isEmpty(rootDir) || !rootDir.startsWith("/"))
        {
            throw new IllegalStateException("qt.pages.root-dir 启用 Pages 时必须是 Linux 绝对路径");
        }
        if (StringUtils.isEmpty(baseDomain) || baseDomain.contains("*"))
        {
            throw new IllegalStateException("qt.pages.base-domain 不能为空或包含通配符");
        }
        if (StringUtils.isEmpty(assetBaseUrl) || !assetBaseUrl.matches("^https?://[A-Za-z0-9.-]+(?::[0-9]+)?$"))
        {
            throw new IllegalStateException("qt.pages.asset-base-url 必须是无路径的 http(s) 绝对地址");
        }
        if (pollIntervalSeconds < 60)
        {
            throw new IllegalStateException("qt.pages.poll-interval-seconds 至少 60 秒");
        }
        if (scanIntervalSeconds < 1)
        {
            throw new IllegalStateException("qt.pages.scan-interval-seconds 至少 1 秒");
        }
        if (pollExecutorThreads < 1 || deployExecutorThreads < 1 || executorQueueSize < 1)
        {
            throw new IllegalStateException("qt.pages 执行器线程与队列参数必须为正数");
        }
        if (downloadLimitBytes <= 0 || extractedLimitBytes <= 0 || maxFileBytes <= 0)
        {
            throw new IllegalStateException("qt.pages 下载与文件大小上限必须为正数");
        }
        if (extractedLimitBytes < downloadLimitBytes)
        {
            throw new IllegalStateException("qt.pages.extracted-limit-bytes 不能小于 download-limit-bytes");
        }
        if (maxFiles <= 0 || maxEntries <= 0)
        {
            throw new IllegalStateException("qt.pages 文件与条目数量上限必须为正数");
        }
        if (connectTimeoutSeconds <= 0 || downloadTimeoutSeconds <= 0 || deployTimeoutSeconds <= 0)
        {
            throw new IllegalStateException("qt.pages 超时参数必须为正数");
        }
        if (maxAttempts < 1 || maxAttempts > 5)
        {
            throw new IllegalStateException("qt.pages.max-attempts 允许 1-5");
        }
        if (retryBackoffSeconds == null || retryBackoffSeconds.isEmpty()
                || retryBackoffSeconds.stream().anyMatch(seconds -> seconds == null || seconds <= 0))
        {
            throw new IllegalStateException("qt.pages.retry-backoff-seconds 必须为正整数列表");
        }
        if (retainReleases < 1)
        {
            throw new IllegalStateException("qt.pages.retain-releases 至少保留 1 个版本");
        }
        if (diskQuotaBytes <= 0)
        {
            throw new IllegalStateException("qt.pages.disk-quota-bytes 必须为正数");
        }
        if (logLimitBytes < 1024 || logLimitBytes > 64 * 1024)
        {
            throw new IllegalStateException("qt.pages.log-limit-bytes 允许 1KiB-64KiB");
        }
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public String getBaseDomain()
    {
        return baseDomain;
    }

    public void setBaseDomain(String baseDomain)
    {
        this.baseDomain = baseDomain;
    }

    public String getAssetBaseUrl()
    {
        return assetBaseUrl;
    }

    public void setAssetBaseUrl(String assetBaseUrl)
    {
        this.assetBaseUrl = assetBaseUrl == null ? null : assetBaseUrl.trim().replaceAll("/+$", "");
    }

    public String getRootDir()
    {
        return rootDir;
    }

    public void setRootDir(String rootDir)
    {
        this.rootDir = rootDir;
    }

    public String getGithubToken()
    {
        return githubToken;
    }

    public void setGithubToken(String githubToken)
    {
        this.githubToken = githubToken;
    }

    public int getPollIntervalSeconds()
    {
        return pollIntervalSeconds;
    }

    public void setPollIntervalSeconds(int pollIntervalSeconds)
    {
        this.pollIntervalSeconds = pollIntervalSeconds;
    }

    public int getScanIntervalSeconds()
    {
        return scanIntervalSeconds;
    }

    public void setScanIntervalSeconds(int scanIntervalSeconds)
    {
        this.scanIntervalSeconds = scanIntervalSeconds;
    }

    public int getPollExecutorThreads()
    {
        return pollExecutorThreads;
    }

    public void setPollExecutorThreads(int pollExecutorThreads)
    {
        this.pollExecutorThreads = pollExecutorThreads;
    }

    public int getDeployExecutorThreads()
    {
        return deployExecutorThreads;
    }

    public void setDeployExecutorThreads(int deployExecutorThreads)
    {
        this.deployExecutorThreads = deployExecutorThreads;
    }

    public int getExecutorQueueSize()
    {
        return executorQueueSize;
    }

    public void setExecutorQueueSize(int executorQueueSize)
    {
        this.executorQueueSize = executorQueueSize;
    }

    public long getDownloadLimitBytes()
    {
        return downloadLimitBytes;
    }

    public void setDownloadLimitBytes(long downloadLimitBytes)
    {
        this.downloadLimitBytes = downloadLimitBytes;
    }

    public long getExtractedLimitBytes()
    {
        return extractedLimitBytes;
    }

    public void setExtractedLimitBytes(long extractedLimitBytes)
    {
        this.extractedLimitBytes = extractedLimitBytes;
    }

    public int getMaxFiles()
    {
        return maxFiles;
    }

    public void setMaxFiles(int maxFiles)
    {
        this.maxFiles = maxFiles;
    }

    public int getMaxEntries()
    {
        return maxEntries;
    }

    public void setMaxEntries(int maxEntries)
    {
        this.maxEntries = maxEntries;
    }

    public long getMaxFileBytes()
    {
        return maxFileBytes;
    }

    public void setMaxFileBytes(long maxFileBytes)
    {
        this.maxFileBytes = maxFileBytes;
    }

    public int getConnectTimeoutSeconds()
    {
        return connectTimeoutSeconds;
    }

    public void setConnectTimeoutSeconds(int connectTimeoutSeconds)
    {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    public int getDownloadTimeoutSeconds()
    {
        return downloadTimeoutSeconds;
    }

    public void setDownloadTimeoutSeconds(int downloadTimeoutSeconds)
    {
        this.downloadTimeoutSeconds = downloadTimeoutSeconds;
    }

    public int getDeployTimeoutSeconds()
    {
        return deployTimeoutSeconds;
    }

    public void setDeployTimeoutSeconds(int deployTimeoutSeconds)
    {
        this.deployTimeoutSeconds = deployTimeoutSeconds;
    }

    public int getMaxAttempts()
    {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts)
    {
        this.maxAttempts = maxAttempts;
    }

    public List<Integer> getRetryBackoffSeconds()
    {
        return retryBackoffSeconds;
    }

    public void setRetryBackoffSeconds(List<Integer> retryBackoffSeconds)
    {
        this.retryBackoffSeconds = retryBackoffSeconds;
    }

    public int getRetainReleases()
    {
        return retainReleases;
    }

    public void setRetainReleases(int retainReleases)
    {
        this.retainReleases = retainReleases;
    }

    public long getDiskQuotaBytes()
    {
        return diskQuotaBytes;
    }

    public void setDiskQuotaBytes(long diskQuotaBytes)
    {
        this.diskQuotaBytes = diskQuotaBytes;
    }

    public int getLogLimitBytes()
    {
        return logLimitBytes;
    }

    public void setLogLimitBytes(int logLimitBytes)
    {
        this.logLimitBytes = logLimitBytes;
    }

    @Override
    public String toString()
    {
        return "QtPagesProperties{qt-pages["
                + "enabled=" + enabled
                + ", baseDomain=" + baseDomain
                + ", rootDir=" + rootDir
                + ", githubToken=" + (StringUtils.isEmpty(githubToken) ? "<empty>" : "<configured>")
                + ", pollIntervalSeconds=" + pollIntervalSeconds
                + ", scanIntervalSeconds=" + scanIntervalSeconds
                + ", pollExecutorThreads=" + pollExecutorThreads
                + ", deployExecutorThreads=" + deployExecutorThreads
                + ", executorQueueSize=" + executorQueueSize
                + ", downloadLimitBytes=" + downloadLimitBytes
                + ", extractedLimitBytes=" + extractedLimitBytes
                + ", maxFiles=" + maxFiles
                + ", maxEntries=" + maxEntries
                + ", maxFileBytes=" + maxFileBytes
                + ", connectTimeoutSeconds=" + connectTimeoutSeconds
                + ", downloadTimeoutSeconds=" + downloadTimeoutSeconds
                + ", deployTimeoutSeconds=" + deployTimeoutSeconds
                + ", maxAttempts=" + maxAttempts
                + ", retryBackoffSeconds=" + retryBackoffSeconds
                + ", retainReleases=" + retainReleases
                + ", diskQuotaBytes=" + diskQuotaBytes
                + ", logLimitBytes=" + logLimitBytes
                + "]}";
    }
}
