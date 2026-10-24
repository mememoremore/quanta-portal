package com.ruoyi.qt.config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

/**
 * Pages 专用有界执行器。仅在 qt.pages.enabled=true 时创建；
 * GitHub 检查与文件部署分离，队列满时抛出拒绝异常，由调用方保留数据库 PENDING 任务，
 * 等待下一次扫描，保证任务不丢失。
 */
@Configuration
@ConditionalOnProperty(prefix = "qt.pages", name = "enabled", havingValue = "true")
public class QtPagesExecutorConfig
{
    @Bean(destroyMethod = "shutdown")
    public ExecutorService qtPagesPollExecutor(QtPagesProperties properties)
    {
        return bounded("poll", properties.getPollExecutorThreads(), properties.getExecutorQueueSize());
    }

    @Bean(destroyMethod = "shutdown")
    public ExecutorService qtPagesDeployExecutor(QtPagesProperties properties)
    {
        return bounded("deploy", properties.getDeployExecutorThreads(), properties.getExecutorQueueSize());
    }

    private static ExecutorService bounded(String name, int threads, int queueSize)
    {
        ThreadFactory threadFactory = new CustomizableThreadFactory("qt-pages-" + name + "-");
        ThreadPoolExecutor executor = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize), threadFactory, new ThreadPoolExecutor.AbortPolicy());
        executor.allowCoreThreadTimeOut(false);
        return executor;
    }
}
