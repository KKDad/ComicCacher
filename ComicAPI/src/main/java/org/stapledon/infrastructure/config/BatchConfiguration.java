package org.stapledon.infrastructure.config;

import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;

import lombok.RequiredArgsConstructor;

/**
 * Configuration for Spring Batch processing.
 * Note: @EnableBatchProcessing is not used in Spring Boot 3.x as it disables auto-configuration.
 * Spring Boot will automatically configure batch processing when spring-boot-starter-batch is present.
 */
@Configuration
@RequiredArgsConstructor
public class BatchConfiguration {

    /**
     * Job launcher configuration for synchronous execution
     */
    @Bean
    public JobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(new SyncTaskExecutor()); // Synchronous execution for simplicity
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }
}