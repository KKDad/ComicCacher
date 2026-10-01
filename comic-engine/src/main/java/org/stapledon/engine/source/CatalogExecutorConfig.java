package org.stapledon.engine.source;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

import org.stapledon.common.util.MdcTaskDecorator;

/**
 * Provides {@code catalogTaskExecutor}: one background thread for the Sources page's on-demand work (catalog thumbnails, avatars of newly added comics,
 * start-date detection), so a request never waits on a source's throttle. One thread keeps that work from competing with itself; every request is
 * still paced by {@code SourceThrottleService}. A full queue drops new work rather than blocking the request that asked for it.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class CatalogExecutorConfig {

    static final int QUEUE_CAPACITY = 200;

    /**
     * The single-threaded executor for on-demand source work.
     */
    @Bean(name = "catalogTaskExecutor", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor catalogTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("catalog-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        log.info("catalogTaskExecutor initialized: 1 thread, queue={}", QUEUE_CAPACITY);
        return executor;
    }
}
