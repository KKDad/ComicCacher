package org.stapledon.infrastructure.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;

import lombok.RequiredArgsConstructor;

/**
 * Configuration for Spring Batch processing.
 * Uses in-memory job repository for simplicity - can be changed to database later.
 */
@Configuration
@EnableBatchProcessing
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