package org.stapledon.batch;

import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;
import org.stapledon.engine.batch.JsonBatchExecutionTracker;

/**
 * Minimal test-only batch job for verifying log file placement via the SiftingAppender.
 */
@Configuration
@Profile("batch-integration")
public class TestLogVerificationJobConfig {

    @Bean
    public Job logVerificationJob(JobRepository jobRepository,
                                  @Qualifier("logVerificationStep") Step step,
                                  JsonBatchExecutionTracker tracker) {
        return new JobBuilder("LogVerificationJob", jobRepository)
                .listener(tracker)
                .start(step)
                .build();
    }

    @Bean
    public Step logVerificationStep(JobRepository jobRepository,
                                    PlatformTransactionManager txManager) {
        return new StepBuilder("logVerificationStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    LoggerFactory.getLogger("org.stapledon.batch.LogVerificationJob")
                            .info("Log verification marker");
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }
}
