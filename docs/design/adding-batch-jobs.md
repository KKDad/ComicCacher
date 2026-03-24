# Adding New Batch Jobs

Step-by-step guide for adding a new batch job to ComicCacher using the scheduler framework.

## Overview

The batch job system uses:
- **`DailyJobScheduler`**: For cron-scheduled jobs (run once per day, with missed execution detection)
- **`PeriodicJobScheduler`**: For fixed-delay jobs (available but currently unused)
- **`JobOperator`**: Spring Batch 6 API for job execution
- **`JsonBatchExecutionTracker`**: Tracks executions to `batch-executions.json`
- **`SchedulerTriggers`**: Centralized `@Scheduled` methods for all cron triggers
- **`SchedulerStateWiring`**: Injects pause/resume support into all `DailyJobScheduler` beans

## Step-by-Step Guide

### 1. Create the Job Configuration

Create a new file in `comic-engine/src/main/java/org/stapledon/engine/batch/config/`:

```java
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.my-job.enabled", havingValue = "true", matchIfMissing = true)
public class MyNewJobConfig {

    private final MyService myService;

    @Value("${batch.my-job.cron}")
    private String cronExpression;

    @Value("${batch.timezone:America/Toronto}")
    private String timezone;

    /**
     * Scheduler for MyNewJob - runs daily at configured cron time.
     * Triggered by SchedulerTriggers component.
     */
    @Bean
    public DailyJobScheduler myNewJobScheduler(
            @Qualifier("myNewJob") Job myNewJob,
            JobOperator jobOperator,
            JsonBatchExecutionTracker tracker) {
        return new DailyJobScheduler(
                myNewJob,
                cronExpression,
                timezone,
                jobOperator,
                tracker,
                "Human-readable description of what this job does");
    }

    /**
     * Job definition
     */
    @Bean
    public Job myNewJob(
            JobRepository jobRepository,
            @Qualifier("myNewStep") Step myNewStep,
            JsonBatchExecutionTracker jsonBatchExecutionTracker) {

        return new JobBuilder("MyNewJob", jobRepository)
                .listener(jsonBatchExecutionTracker)
                .start(myNewStep)
                .build();
    }

    /**
     * Step definition (tasklet pattern)
     */
    @Bean
    public Step myNewStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager) {

        return new StepBuilder("myNewStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    myService.doWork();
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
```

Key points about the `DailyJobScheduler` constructor:

```java
// Without description
new DailyJobScheduler(job, cronExpression, timezone, jobOperator, executionTracker)

// With description (preferred -- shown in UI)
new DailyJobScheduler(job, cronExpression, timezone, jobOperator, executionTracker, description)
```

The `description` parameter is displayed in the batch jobs UI. Always provide one.

### 2. Add Configuration Properties

Add to `application.properties`:

```properties
# My New Job Configuration
batch.my-job.enabled=true
batch.my-job.cron=0 0 8 * * ?
```

Use `matchIfMissing = true` on the `@ConditionalOnProperty` if the job should be enabled by default. Use `matchIfMissing = false` for jobs that should be opt-in (like `AvatarBackfillJob`).

### 3. Register in KNOWN_JOBS

Add your job name to `BatchJobBaseConfig.KNOWN_JOBS` in `comic-engine/src/main/java/org/stapledon/engine/batch/BatchJobBaseConfig.java`:

```java
public static final Set<String> KNOWN_JOBS = Set.of(
        "AvatarBackfillJob",
        "ComicBackfillJob",
        "ComicDownloadJob",
        "ImageMetadataBackfillJob",
        "MetricsArchiveJob",
        "MyNewJob",                  // <-- Add here
        "RetrievalRecordPurgeJob"
);
```

This set is used by the scheduler health check to detect missing or unexpected schedulers at startup.

### 4. Add Cron Constants (Optional)

Add default cron and property key constants in `BatchJobBaseConfig`:

```java
public static final class CronSchedules {
    // ... existing entries
    /** Daily at 8:00 AM EST - My new job */
    public static final String MY_NEW_JOB = "0 0 8 * * ? " + BATCH_TIMEZONE;
}

public static final class PropertyKeys {
    // ... existing entries
    public static final String MY_NEW_JOB_ENABLED = "batch.my-job.enabled";
    public static final String MY_NEW_JOB_CRON = "batch.my-job.cron";
}
```

### 5. Add Trigger in SchedulerTriggers

Add a scheduling trigger method in `comic-engine/src/main/java/org/stapledon/engine/batch/scheduler/SchedulerTriggers.java`:

1. Add a constructor parameter:

```java
public SchedulerTriggers(
        // ... existing parameters
        @Qualifier("myNewJobScheduler") DailyJobScheduler myNewJobScheduler) {
    // ... existing assignments
    this.myNewJobScheduler = myNewJobScheduler;
}
```

2. Add the `@Scheduled` method:

```java
@Scheduled(cron = "${batch.my-job.cron}", zone = "${batch.timezone}")
@ConditionalOnProperty(name = "batch.my-job.enabled", havingValue = "true", matchIfMissing = true)
public void triggerMyNewJob() {
    if (myNewJobScheduler != null) {
        myNewJobScheduler.executeScheduled();
    }
}
```

### 6. SchedulerStateWiring

No action needed. `SchedulerStateWiring` auto-discovers all `DailyJobScheduler` beans via `@Autowired(required = false) List<DailyJobScheduler>` and injects `SchedulerStateService` into each one. Your new scheduler will be automatically wired.

## Job Patterns

### Tasklet Pattern (Simple)

Use for jobs that perform a single operation. Examples: `MetricsArchiveJob`, `RetrievalRecordPurgeJob`, `AvatarBackfillJob`.

```java
@Bean
public Step myStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
    return new StepBuilder("myStep", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                myService.doWork();
                return RepeatStatus.FINISHED;
            }, transactionManager)
            .build();
}
```

### Chunk-Oriented Pattern (Reader/Processor/Writer)

Use for jobs that process items in batches. Examples: `ComicDownloadJob` (chunk size 1), `ComicBackfillJob` (configurable chunk size).

```java
@Bean
public Step myStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
        ItemReader<Input> reader, ItemProcessor<Input, Output> processor, ItemWriter<Output> writer) {
    return new StepBuilder("myStep", jobRepository)
            .<Input, Output>chunk(chunkSize)
            .transactionManager(transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
}
```

Use `@StepScope` on the reader bean if the data source should be evaluated at job run time rather than at startup (see `ComicBackfillJobConfig.backfillTaskReader()`).

## Testing

### Integration Test

Create an IT class in `comic-api/src/integration/java/`:

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

# Run unit tests
./gradlew :comic-engine:test

# Run integration tests
./gradlew :comic-api:integrationTest

# Full verification
./gradlew clean testAll

# Runtime health check
curl http://localhost:8080/actuator/health | jq '.components.schedulerHealthCheck'
```

## Checklist

- [ ] Job config class in `comic-engine/.../batch/config/`
- [ ] `@ConditionalOnProperty` on the config class with correct `matchIfMissing`
- [ ] `DailyJobScheduler` bean with description
- [ ] Job bean with `JsonBatchExecutionTracker` listener
- [ ] Job name added to `BatchJobBaseConfig.KNOWN_JOBS`
- [ ] Properties added to `application.properties` (`enabled` + `cron`)
- [ ] Constructor parameter and `@Scheduled` method added to `SchedulerTriggers`
- [ ] Integration test
- [ ] `./gradlew clean testAll` passes
