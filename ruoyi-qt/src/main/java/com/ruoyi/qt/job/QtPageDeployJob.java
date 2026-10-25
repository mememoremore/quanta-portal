package com.ruoyi.qt.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.ruoyi.qt.service.IQtPageDeployService;

/**
 * Pages 部署任务扫描：每 scan-interval-seconds 领取一次到期 PENDING 任务并提交到
 * 专用有界执行器；队列满时任务保持 PENDING，下一轮再领，数据库是任务唯一事实来源。
 */
@Component
@ConditionalOnProperty(prefix = "qt.pages", name = "enabled", havingValue = "true")
public class QtPageDeployJob
{
    private static final Logger log = LoggerFactory.getLogger(QtPageDeployJob.class);

    private final IQtPageDeployService deployService;

    public QtPageDeployJob(IQtPageDeployService deployService)
    {
        this.deployService = deployService;
    }

    @Scheduled(initialDelayString = "10000", fixedDelayString = "${qt.pages.scan-interval-seconds:5}000")
    public void scan()
    {
        try
        {
            deployService.dispatchPending();
        }
        catch (Exception e)
        {
            log.error("Pages 任务扫描失败", e);
        }
    }
}
