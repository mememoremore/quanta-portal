package com.ruoyi.qt.pages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import com.ruoyi.qt.service.IQtPageDeployService;

/**
 * Pages 启动恢复：先对账 PUBLISHING 与中断任务，再放行本人写接口与调度。
 * 单站点对账失败只影响该站点（暂停或保持原版本），不阻断整体就绪。
 */
@Component
@ConditionalOnProperty(prefix = "qt.pages", name = "enabled", havingValue = "true")
public class QtPagesStartupRecovery implements ApplicationRunner
{
    private static final Logger log = LoggerFactory.getLogger(QtPagesStartupRecovery.class);

    private final QtPagesRuntime runtime;

    private final IQtPageDeployService deployService;

    public QtPagesStartupRecovery(QtPagesRuntime runtime, IQtPageDeployService deployService)
    {
        this.runtime = runtime;
        this.deployService = deployService;
    }

    @Override
    public void run(ApplicationArguments args)
    {
        log.info("Quanta Pages 启动恢复开始");
        try
        {
            deployService.reconcilePublishing();
        }
        catch (Exception e)
        {
            log.error("Pages PUBLISHING 对账失败", e);
        }
        try
        {
            deployService.recoverInterrupted();
        }
        catch (Exception e)
        {
            log.error("Pages 中断任务恢复失败", e);
        }
        runtime.markReady();
        log.info("Quanta Pages 启动恢复完成，已就绪");
    }
}
