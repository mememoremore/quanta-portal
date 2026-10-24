package com.ruoyi.qt.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.ruoyi.qt.service.IQtPageDeployService;

/**
 * Pages 分支轮询：每 poll-interval-seconds 扫描一次到期站点。
 * 网络请求在 QtPageDeployServiceImpl 内部于事务外完成，不阻塞共享调度线程池。
 */
@Component
@ConditionalOnProperty(prefix = "qt.pages", name = "enabled", havingValue = "true")
public class QtPageRepositoryPollJob
{
    private static final Logger log = LoggerFactory.getLogger(QtPageRepositoryPollJob.class);

    private final IQtPageDeployService deployService;

    public QtPageRepositoryPollJob(IQtPageDeployService deployService)
    {
        this.deployService = deployService;
    }

    @Scheduled(initialDelayString = "20000", fixedDelayString = "${qt.pages.poll-interval-seconds:300}000")
    public void poll()
    {
        try
        {
            deployService.pollDueSites();
        }
        catch (Exception e)
        {
            log.error("Pages 分支轮询失败", e);
        }
    }
}
