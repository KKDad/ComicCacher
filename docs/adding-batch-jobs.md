# Adding New Batch Jobs

This guide explains how to add new batch jobs to ComicCacher using the scheduler framework.

## Overview

The batch job system uses:
- **`DailyJobScheduler`**: For cron-scheduled jobs (run once per day)
- **`PeriodicJobScheduler`**: For fixed-delay jobs (run continuously)
- **`JobOperator`**: Spring Batch 6 API for job execution
- **`JsonBatchExecutionTracker`**: Tracks executions to `batch-executions.json`

## Step-by-Step Guide

### 1. Create the Job Configuration

Create a new file in `comic-engine/src/main/java/org/stapledon/engine/batch/config/`:

```java
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MyNewJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JsonBatchExecutionTracker executionTracker;
    private final JobOperator jobOperator;

    // Your business logic dependencies
    private final MyService myService;

    @Value("${batch.my-job.cron}")
    private String cronExpression;

    @Value("${batch.timezone}")
    private String timezone;

    @Bean
    public Job myNewJob() {
        return new JobBuilder("MyNewJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(executionTracker)
                .start(myNewStep())
                .build();
    }

    @Bean
    public Step myNewStep() {
        return new StepBuilder("myNewStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    // Your job logic here
                    myService.doWork();
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "batch.my-job.enabled", havingValue = "true", matchIfMissing = true)
    public DailyJobScheduler myNewJobScheduler() {
        return new DailyJobScheduler(
                "MyNewJob",
                cronExpression,
                timezone,
                jobOperator,
                executionTracker);
    }
}
```

### 2. Add Configuration Properties

Add to `application.properties`:

```properties
# My New Job Configuration
batch.my-job.enabled=true
batch.my-job.cron=0 0 7 * * ?
```

### 3. Register in KNOWN_JOBS

Add your job name to `BatchJobBaseConfig.KNOWN_JOBS`:

```java
public static final Set<String> KNOWN_JOBS = Set.of(
        "ComicBackfillJob",
        "ComicDownloadJob",
        // ... existing jobs
        "MyNewJob"  // Add here
);
```

### 4. Add Trigger (for cron jobs)

Add scheduling trigger in `SchedulerTriggers`:

```java
@Scheduled(cron = "${batch.my-job.cron}", zone = "${batch.timezone}")
public void triggerMyNewJob() {
    Optional.ofNullable(schedulers.get("myNewJobScheduler"))
            .ifPresent(scheduler -> ((DailyJobScheduler) scheduler).executeScheduled());
}
```

## Job Types

### Daily Jobs (DailyJobScheduler)

Use for jobs that run once per day:
- Automatic missed execution detection on startup
- Prevents duplicate runs within same day
- Uses cron expression for scheduling

### Periodic Jobs (PeriodicJobScheduler)

Use for jobs that run continuously:
- Fixed delay between executions
- No missed execution logic
- Good for polling/monitoring tasks

## Testing

1. **Unit Test**: Test job configuration and steps
2. **Integration Test**: Create IT class extending `AbstractBatchJobIntegrationTest`

Example integration test:

```java
@TestPropertySource(properties = {"batch.my-job.enabled=true"})
class MyNewJobIT extends AbstractBatchJobIntegrationTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("myNewJob")
    private Job myNewJob;

    @Test
    void testMyNewJobCompletes() throws Exception {
        JobExecution execution = jobLauncher.run(myNewJob,
                new JobParametersBuilder()
                        .addLong("runId", System.currentTimeMillis())
                        .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
}
```

## Verification

After implementation:

```bash
# Compile
./gradlew :comic-engine:compileJava

# Run tests
./gradlew :comic-api:integrationTest

# Check health
curl http://localhost:8080/actuator/health | jq '.components.schedulerHealthCheck'
```
