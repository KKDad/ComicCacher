package org.stapledon.engine.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.support.TaskExecutorJobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;

/**
 * Starts manually triggered jobs on {@code manualJobTaskExecutor} and returns their execution at once, so a "Run now" doesn't hold the GraphQL
 * request (and a Tomcat thread) for the whole run. Scheduled runs keep the application's synchronous {@code JobOperator}.
 * <p>
 * Not a {@code JobOperator} bean: a second one would make every by-type injection of the application's operator ambiguous.
 */
@Slf4j
@Component
public class ManualJobLauncher {

    private final JobRepository jobRepository;
    private final TaskExecutor executor;

    public ManualJobLauncher(JobRepository jobRepository, @Qualifier("manualJobTaskExecutor") TaskExecutor executor) {
        this.jobRepository = jobRepository;
        this.executor = executor;
    }

    /**
     * Creates the job's execution and runs it in the background.
     *
     * @param onEnd runs once the job has ended, or when it couldn't be queued; not when this method throws before queuing it
     * @return the new execution, usually still STARTING
     */
    public JobExecution start(Job job, JobParameters parameters, Runnable onEnd) throws Exception {
        TaskExecutorJobOperator operator = new TaskExecutorJobOperator();
        operator.setJobRepository(jobRepository);
        // start(Job, JobParameters) never looks a job up, but the operator requires a registry
        operator.setJobRegistry(new MapJobRegistry());
        operator.setTaskExecutor(task -> {
            try {
                executor.execute(() -> {
                    try {
                        task.run();
                    } finally {
                        onEnd.run();
                    }
                });
            } catch (TaskRejectedException e) {
                // The operator marks the execution FAILED and doesn't rethrow, so the job never runs
                log.warn("Could not queue {}: {}", job.getName(), e.toString());
                onEnd.run();
                throw e;
            }
        });
        operator.afterPropertiesSet();
        return operator.start(job, parameters);
    }
}
