package com.ruoyi.qt.pages;

import java.nio.file.FileSystems;
import java.nio.file.Paths;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.qt.config.QtPagesProperties;

/**
 * Pages 运行时：持有发布存储、GitHub 客户端与归档校验器。
 *
 * 功能关闭时不创建任何组件、不触碰磁盘与网络；启用时先完成 POSIX/目录校验，
 * 启动恢复（QtPagesStartupRecovery）完成后才 markReady，此前本人写接口返回 503。
 */
@Component
public class QtPagesRuntime
{
    private static final Logger log = LoggerFactory.getLogger(QtPagesRuntime.class);

    private final QtPagesProperties properties;

    private volatile boolean ready;

    private volatile QtPageReleaseStore store;

    private volatile QtGitHubClient gitHubClient;

    private volatile QtStaticArchiveValidator validator;

    public QtPagesRuntime(QtPagesProperties properties)
    {
        this.properties = properties;
    }

    @PostConstruct
    void initialize()
    {
        if (!properties.isEnabled())
        {
            return;
        }
        if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix"))
        {
            throw new IllegalStateException("qt.pages.enabled 需要 POSIX 文件系统（Linux），当前环境不支持");
        }
        // 磁盘布局 {rootDir}/sites/{slug}/，与 nginx 模板和 prepare-pages.sh 一致
        this.store = new QtPageReleaseStore(Paths.get(properties.getRootDir(), "sites"));
        this.gitHubClient = new QtGitHubClient(properties);
        this.validator = new QtStaticArchiveValidator(properties.getExtractedLimitBytes(),
                properties.getMaxFileBytes(), properties.getMaxFiles(), properties.getMaxEntries());
        log.info("Quanta Pages 运行时已初始化，等待启动恢复完成: {}", properties);
    }

    /** 本人写接口前置检查：关闭或恢复未完成一律 503。 */
    public void requireReady()
    {
        if (!properties.isEnabled())
        {
            throw new ServiceException("个人网站功能未开启", HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (!ready)
        {
            throw new ServiceException("个人网站功能正在恢复，请稍后再试", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    public boolean isEnabled()
    {
        return properties.isEnabled();
    }

    public boolean isReady()
    {
        return properties.isEnabled() && ready;
    }

    public void markReady()
    {
        this.ready = true;
    }

    public QtPagesProperties properties()
    {
        return properties;
    }

    public QtPageReleaseStore store()
    {
        return store;
    }

    public QtGitHubClient gitHubClient()
    {
        return gitHubClient;
    }

    public QtStaticArchiveValidator validator()
    {
        return validator;
    }
}
