package org.stapledon.engine.batch;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.MDC;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.util.DateTimeUtils;
import org.stapledon.common.util.NfsFileOperations;
import org.stapledon.engine.batch.dto.BatchExecutionSummary;
import org.stapledon.engine.batch.dto.BatchStepSummary;

/**
 * Exports Spring Batch job execution data to JSON file for monitoring and
 * history tracking.
 * Implements JobExecutionListener to automatically export after each job
 * completion.
 * Uses gsonWithLocalDate bean for proper OffsetDateTime serialization.
 *
 * <p>Keeps each job's executions from the last {@code batch.tracking.history-days} days (default 30), and always its newest one,
 * so a job that runs rarely still shows its last run. History for a job that no longer exists is dropped. Handles migration from
 * the legacy single-entry format automatically on read.
 *
 * <p>Spring Batch returns {@link LocalDateTime} from {@code JobExecution} and
 * {@code StepExecution}, recorded in the JVM's default zone. Per CLAUDE.md, those
 * values are converted to {@link OffsetDateTime} in {@code batch.timezone} at this
 * boundary (see {@link DateTimeUtils#toOffset}) so persisted data carries an explicit offset.
 */
@Slf4j
@Component
public class JsonBatchExecutionTracker extends LoggingJobExecutionListener implements JobExecutionListener {

    private final CacheProperties cacheProperties;
    private final Gson gson;
    private final int historyDays;
    private final int maxHistoryPerJob;
    private final Supplier<Set<String>> knownJobNames;
    private final ZoneId batchZone;
    private final Clock clock;
    private final ConcurrentHashMap<Long, Long> h2ToStableId = new ConcurrentHashMap<>();

    private static final String BATCH_EXECUTIONS_FILENAME = "batch-executions.json";
    /** Size guard: no job keeps more executions than this, however recent. */
    private static final int MAX_HISTORY_PER_JOB = 500;
    private static final String MDC_EXECUTION_ID = "batchJobExecutionId";
    private static final String MDC_JOB_NAME = "batchJobName";
    private static final String MDC_LOG_PATH = "batchLogPath";

    /**
     * Constructor with configurable history retention. The jobs are looked up when history is pruned, not at construction: each job
     * bean depends on this listener.
     */
    @Autowired
    public JsonBatchExecutionTracker(
            CacheProperties cacheProperties,
            @Qualifier("gsonWithLocalDate") Gson gson,
            @Value("${batch.tracking.history-days:30}") int historyDays,
            Clock clock,
            ObjectProvider<Job> jobs) {
        this(cacheProperties, gson, historyDays, MAX_HISTORY_PER_JOB, clock.getZone().getId(), clock,
                () -> jobs.stream().map(Job::getName).collect(Collectors.toSet()));
    }

    JsonBatchExecutionTracker(CacheProperties cacheProperties, Gson gson, int historyDays, int maxHistoryPerJob, String batchTimezone, Clock clock,
            Supplier<Set<String>> knownJobNames) {
        this.cacheProperties = cacheProperties;
        this.gson = gson;
        this.historyDays = historyDays;
        this.maxHistoryPerJob = maxHistoryPerJob;
        this.knownJobNames = knownJobNames;
        this.batchZone = ZoneId.of(batchTimezone);
        this.clock = clock;
    }

    private OffsetDateTime toOffset(LocalDateTime localDateTime) {
        return DateTimeUtils.toOffset(localDateTime, batchZone);
    }

    @Override
    public synchronized void beforeJob(JobExecution jobExecution) {
        // Set MDC before super.beforeJob() so the start banner is captured in the per-execution log file
        MDC.put(MDC_EXECUTION_ID, String.valueOf(jobExecution.getId()));
        MDC.put(MDC_JOB_NAME, jobExecution.getJobInstance().getJobName());
        String jobName = jobExecution.getJobInstance().getJobName();
        String date = LocalDate.now(clock.withZone(batchZone)).format(DateTimeFormatter.BASIC_ISO_DATE);
        String shortHash = UUID.randomUUID().toString().substring(0, 8);
        String logFileName = jobName + "-" + date + "-" + shortHash;
        MDC.put(MDC_LOG_PATH, jobName + "/" + logFileName);
        super.beforeJob(jobExecution);

        try {
            long stableId = nextStableId();
            h2ToStableId.put(jobExecution.getId(), stableId);

            BatchExecutionSummary summary = BatchExecutionSummary.builder()
                    .executionId(stableId)
                    .jobName(jobName)
                    .status(BatchStatus.STARTED.name())
                    .startTime(toOffset(jobExecution.getStartTime()))
                    .build();

            Map<String, List<BatchExecutionSummary>> executions = readExecutions();
            List<BatchExecutionSummary> jobHistory = executions.computeIfAbsent(jobName, k -> new ArrayList<>());
            jobHistory.addFirst(summary);
            prune(executions);

            writeExecutions(executions);
            log.info("Batch job started: {} - Stable execution ID: {}", jobName, stableId);
        } catch (Exception e) {
            log.error("Failed to write STARTED entry to JSON", e);
        }
    }

    @Override
    public synchronized void afterJob(JobExecution jobExecution) {
        try {
            // Log end banner before MDC cleanup so it's captured in the per-execution log file
            super.afterJob(jobExecution);

            Long stableId = h2ToStableId.remove(jobExecution.getId());
            BatchExecutionSummary summary = createSummary(jobExecution, stableId);
            String logPath = MDC.get(MDC_LOG_PATH);
            if (logPath != null && logPath.contains("/")) {
                summary.setLogFileName(logPath.substring(logPath.lastIndexOf('/') + 1) + ".log");
            }

            Map<String, List<BatchExecutionSummary>> executions = readExecutions();
            String jobName = jobExecution.getJobInstance().getJobName();
            List<BatchExecutionSummary> jobHistory = executions.computeIfAbsent(jobName, k -> new ArrayList<>());

            if (stableId != null) {
                // Find and update the STARTED entry written by beforeJob()
                Optional<BatchExecutionSummary> existing = jobHistory.stream()
                        .filter(s -> stableId.equals(s.getExecutionId()))
                        .findFirst();
                if (existing.isPresent()) {
                    updateSummaryInPlace(existing.get(), summary);
                } else {
                    jobHistory.addFirst(summary);
                }
            } else {
                // Fallback: no beforeJob() entry (e.g. backward compat or beforeJob failed)
                jobHistory.addFirst(summary);
            }

            prune(executions);

            writeExecutions(executions);
            logExecutionSummary(jobName, summary);

        } catch (Exception e) {
            log.error("Failed to export batch execution to JSON", e);
        } finally {
            MDC.remove(MDC_EXECUTION_ID);
            MDC.remove(MDC_JOB_NAME);
            MDC.remove(MDC_LOG_PATH);
        }
    }

    /**
     * Drops history for jobs that no longer exist, then keeps each job's executions that started within the retention window, and
     * always its newest one. Each list is newest first.
     */
    void prune(Map<String, List<BatchExecutionSummary>> executions) {
        Set<String> known = knownJobNames.get();
        if (!known.isEmpty()) {
            executions.keySet().removeIf(jobName -> !known.contains(jobName));
        }

        OffsetDateTime cutoff = OffsetDateTime.now(clock).minusDays(historyDays);
        executions.replaceAll((jobName, history) -> {
            List<BatchExecutionSummary> kept = new ArrayList<>();
            for (int i = 0; i < history.size() && kept.size() < maxHistoryPerJob; i++) {
                BatchExecutionSummary execution = history.get(i);
                if (i == 0 || execution.getStartTime() != null && !execution.getStartTime().isBefore(cutoff)) {
                    kept.add(execution);
                }
            }
            return kept;
        });
    }

    /**
     * Derive the next stable execution ID from the JSON file.
     * Must be called within a synchronized context.
     */
    private long nextStableId() {
        Map<String, List<BatchExecutionSummary>> executions = readExecutions();
        return executions.values().stream()
                .flatMap(List::stream)
                .filter(s -> s.getExecutionId() != null)
                .mapToLong(BatchExecutionSummary::getExecutionId)
                .max()
                .orElse(0L) + 1;
    }

    /**
     * Update an existing STARTED summary in-place with final execution data.
     */
    private void updateSummaryInPlace(BatchExecutionSummary target, BatchExecutionSummary source) {
        target.setStatus(source.getStatus());
        target.setExitCode(source.getExitCode());
        target.setExitMessage(source.getExitMessage());
        target.setStartTime(source.getStartTime());
        target.setEndTime(source.getEndTime());
        target.setErrorMessage(source.getErrorMessage());
        target.setLogFileName(source.getLogFileName());
        target.setParameters(source.getParameters());
        target.setSteps(source.getSteps());
    }

    /**
     * Create execution summary from JobExecution.
     */
    private BatchExecutionSummary createSummary(JobExecution jobExecution, Long stableId) {
        String jobName = jobExecution.getJobInstance().getJobName();
        long effectiveId = Optional.ofNullable(stableId).orElse(nextStableId());

        Map<String, Object> params = new LinkedHashMap<>();
        jobExecution.getJobParameters().parameters()
                .forEach(p -> params.put(p.name(), p.value()));

        List<BatchStepSummary> steps = jobExecution.getStepExecutions().stream()
                .map(step -> new BatchStepSummary(
                        step.getStepName(),
                        step.getStatus().name(),
                        (int) step.getReadCount(),
                        (int) step.getWriteCount(),
                        (int) step.getFilterCount(),
                        (int) step.getSkipCount(),
                        (int) step.getCommitCount(),
                        (int) step.getRollbackCount(),
                        toOffset(step.getStartTime()),
                        toOffset(step.getEndTime())))
                .toList();

        BatchExecutionSummary summary = BatchExecutionSummary.builder()
                .executionId(effectiveId)
                .jobName(jobName)
                .status(jobExecution.getStatus().name())
                .exitCode(jobExecution.getExitStatus().getExitCode())
                .exitMessage(jobExecution.getExitStatus().getExitDescription())
                .startTime(toOffset(jobExecution.getStartTime()))
                .endTime(toOffset(jobExecution.getEndTime()))
                .parameters(params)
                .steps(steps)
                .build();

        if (jobExecution.getStatus() == BatchStatus.FAILED && !jobExecution.getAllFailureExceptions().isEmpty()) {
            Throwable firstException = jobExecution.getAllFailureExceptions().getFirst();
            summary.setErrorMessage(firstException.getMessage());
        }

        return summary;
    }

    /**
     * Read existing executions from JSON file, handling migration from legacy single-entry format.
     */
    private Map<String, List<BatchExecutionSummary>> readExecutions() {
        Path filePath = getExecutionsFilePath();

        if (!Files.exists(filePath)) {
            return new HashMap<>();
        }

        try {
            String json = Files.readString(filePath);
            return parseWithMigration(json);
        } catch (IOException e) {
            log.error("Failed to read batch executions from JSON file", e);
            return new HashMap<>();
        }
    }

    /**
     * Parse JSON, detecting legacy single-entry format and migrating to list format.
     */
    Map<String, List<BatchExecutionSummary>> parseWithMigration(String json) {
        JsonElement parsed = gson.fromJson(json, JsonElement.class);
        if (parsed == null || !parsed.isJsonObject()) {
            return new HashMap<>();
        }
        JsonObject root = parsed.getAsJsonObject();

        // Detect format: if any top-level value is an array, it's the new format
        boolean isNewFormat = root.entrySet().stream()
                .anyMatch(entry -> entry.getValue().isJsonArray());

        if (isNewFormat) {
            Type type = new TypeToken<Map<String, List<BatchExecutionSummary>>>() {
            }.getType();
            Map<String, List<BatchExecutionSummary>> result = gson.fromJson(root, type);
            // Ensure mutable lists
            Map<String, List<BatchExecutionSummary>> mutable = new HashMap<>();
            if (result != null) {
                result.forEach((k, v) -> mutable.put(k, new ArrayList<>(v)));
            }
            return mutable;
        }

        // Legacy format: Map<String, BatchExecutionSummary> — wrap each in a list
        log.info("Migrating batch-executions.json from legacy single-entry format to list format");
        Map<String, List<BatchExecutionSummary>> migrated = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            BatchExecutionSummary legacy = gson.fromJson(entry.getValue(), BatchExecutionSummary.class);
            if (legacy != null) {
                // Backfill jobName if missing in legacy data
                if (legacy.getJobName() == null) {
                    legacy.setJobName(entry.getKey());
                }
                migrated.put(entry.getKey(), new ArrayList<>(List.of(legacy)));
            }
        }
        return migrated;
    }

    /**
     * Write executions to JSON file using atomic write for NFS safety.
     */
    private void writeExecutions(Map<String, List<BatchExecutionSummary>> executions) throws IOException {
        Path filePath = getExecutionsFilePath();
        String json = gson.toJson(executions);
        NfsFileOperations.atomicWrite(filePath, json);
        log.debug("Batch executions exported to: {}", filePath);
    }

    /**
     * Get path to batch executions JSON file.
     */
    private Path getExecutionsFilePath() {
        return Paths.get(cacheProperties.getLocation(), BATCH_EXECUTIONS_FILENAME);
    }

    /**
     * Log execution summary.
     */
    private void logExecutionSummary(String jobName, BatchExecutionSummary summary) {
        Duration duration = Duration.between(
                summary.getStartTime(),
                summary.getEndTime());
        log.info("Batch job completed: {} - Status: {}, Duration: {}ms", jobName, summary.getStatus(), duration.toMillis());

        if (summary.getErrorMessage() != null) {
            log.error("Job {} failed with error: {}", jobName, summary.getErrorMessage());
        }
    }

    // ==================== Public Read Methods ====================

    /**
     * Gets the last execution summary for a specific job.
     */
    public Optional<BatchExecutionSummary> getLastExecution(String jobName) {
        Map<String, List<BatchExecutionSummary>> executions = readExecutions();
        return Optional.ofNullable(executions.get(jobName))
                .filter(list -> !list.isEmpty())
                .map(List::getFirst);
    }

    /**
     * Gets execution history for a specific job.
     */
    public List<BatchExecutionSummary> getExecutionHistory(String jobName, int count) {
        Map<String, List<BatchExecutionSummary>> executions = readExecutions();
        List<BatchExecutionSummary> history = executions.getOrDefault(jobName, List.of());
        return history.stream()
                .limit(count)
                .toList();
    }

    /**
     * Gets execution history for a specific job within a date range.
     */
    public List<BatchExecutionSummary> getExecutionHistoryForDateRange(String jobName, LocalDate start, LocalDate end) {
        Map<String, List<BatchExecutionSummary>> executions = readExecutions();
        List<BatchExecutionSummary> history = executions.getOrDefault(jobName, List.of());
        return history.stream()
                .filter(s -> s.getStartTime() != null)
                .filter(s -> {
                    LocalDate executionDate = s.getStartTime().atZoneSameInstant(batchZone).toLocalDate();
                    return !executionDate.isBefore(start) && !executionDate.isAfter(end);
                })
                .toList();
    }

    /**
     * Gets a specific execution by ID, scanning all jobs. Returns the most recent match.
     */
    public Optional<BatchExecutionSummary> getExecution(long executionId) {
        Map<String, List<BatchExecutionSummary>> executions = readExecutions();
        return executions.values().stream()
                .flatMap(List::stream)
                .filter(s -> s.getExecutionId() != null && s.getExecutionId() == executionId)
                .findFirst();
    }

    /**
     * Checks if a job has already run today (based on recorded end time). "Today" is the date in {@code batch.timezone}, the zone the cron
     * schedules run in, whatever the JVM's default zone is.
     */
    public boolean hasJobRunToday(String jobName) {
        LocalDate today = LocalDate.now(clock.withZone(batchZone));
        return getLastExecution(jobName)
                .filter(summary -> summary.getEndTime() != null)
                .filter(summary -> summary.getEndTime().atZoneSameInstant(batchZone).toLocalDate().equals(today))
                .isPresent();
    }

}
