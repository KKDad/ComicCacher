package org.stapledon.engine.management;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;


/**
 * Provides the {@code sourceDownloadExecutor} bean used by {@link ComicManagementFacade} to run per-source download work in parallel. Pool sized for the known set of comic sources
 * (currently 3: gocomics, comicskingdom, freefall) plus headroom.
 */
@Slf4j
@Configuration
public class SourceDownloadExecutorConfig {

    /**
     * Thread pool used to run one download task per comic source in parallel. Each task processes its source's comics serially, paced by {@code SourceThrottleService}.
     */
    @Bean(name = "sourceDownloadExecutor", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor sourceDownloadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(16);
        executor.setThreadNamePrefix("comic-source-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("sourceDownloadExecutor initialized: core/max={}, queue={}", executor.getCorePoolSize(), executor.getQueueCapacity());
        return executor;
    }
}
