package org.stapledon.engine.batch.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

import org.stapledon.common.util.MdcTaskDecorator;

/**
 * Provides {@code manualJobTaskExecutor}, the threads {@code ManualJobLauncher} runs manually triggered jobs on. Each scheduler allows one run of
 * its job at a time, so a few threads cover every job; the decorator carries the request's {@code req=} and {@code user=} into the job's log lines.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class ManualJobLaunchConfig {

    static final int THREADS = 4;
    static final int QUEUE_CAPACITY = 20;

    /**
     * The executor for manually triggered batch jobs.
     */
    @Bean(name = "manualJobTaskExecutor", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor manualJobTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(THREADS);
        executor.setMaxPoolSize(THREADS);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("manual-job-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        log.info("manualJobTaskExecutor initialized: {} threads, queue={}", THREADS, QUEUE_CAPACITY);
        return executor;
    }
}
