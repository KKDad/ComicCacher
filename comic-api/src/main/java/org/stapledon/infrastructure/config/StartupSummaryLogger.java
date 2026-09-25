package org.stapledon.infrastructure.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.stapledon.common.config.CacheProperties;
import org.stapledon.common.config.properties.DownloaderProperties;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Logs one block when the application is ready: what is running, where its data lives, which jobs are on, and how each source is paced.
 * Answers the first questions of any support session from the log alone. Never logs secrets.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupSummaryLogger {

    private static final List<String> JOBS = List.of("comic-download", "comic-backfill", "avatar-backfill", "image-backfill", "metrics-archive",
            "record-purge");

    private final Environment environment;
    private final BuildVersion buildVersion;
    private final CacheProperties cacheProperties;
    private final DownloaderProperties downloaderProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void logSummary() {
        Path cacheRoot = Path.of(cacheProperties.getLocation()).toAbsolutePath();
        log.info("======== ComicCacher ready ========");
        log.info("  Version: {} (built {}), Java {} ({}), virtual threads: {}", buildVersion.getBuildProperty("build.version"),
                buildVersion.getBuildProperty("build.time"), Runtime.version(), System.getProperty("java.vendor"),
                environment.getProperty("spring.threads.virtual.enabled", "false"));
        log.info("  Profiles: {}", Arrays.toString(environment.getActiveProfiles()));
        log.info("  Cache root: {}", cacheRoot);
        log.info("  Config files: comics={}, users={}, preferences={}", cacheRoot.resolve(cacheProperties.getConfig()).normalize(),
                cacheRoot.resolve(cacheProperties.getUsersConfig()).normalize(), cacheRoot.resolve(cacheProperties.getPreferencesConfig()).normalize());
        for (String job : JOBS) {
            log.info("  Job {}: enabled={}, cron={}", job, environment.getProperty("batch." + job + ".enabled", "false"),
                    environment.getProperty("batch." + job + ".cron", "-"));
        }
        Map<String, DownloaderProperties.Source> sources = downloaderProperties.getSources() == null ? Map.of()
                : new TreeMap<>(downloaderProperties.getSources());
        sources.keySet().forEach(source -> {
            DownloaderProperties.Throttle throttle = downloaderProperties.throttleFor(source);
            DownloaderProperties.Retry retry = downloaderProperties.retryFor(source);
            log.info("  Source {}: delay {}-{}ms, retry max-attempts={} backoff {}-{}ms", source, throttle.getMinDelayMs(), throttle.getMaxDelayMs(),
                    retry.getMaxAttempts(), retry.getInitialBackoffMs(), retry.getMaxBackoffMs());
        });
        log.info("  Metrics: enabled={}, duplicate detection: {} ({})", environment.getProperty("comics.metrics.enabled", "false"),
                cacheProperties.isDuplicateDetectionEnabled(), cacheProperties.getHashAlgorithm());
        String mailHost = environment.getProperty("spring.mail.host");
        log.info("  Mail: {}", StringUtils.hasText(mailHost) ? "configured (" + mailHost + ")" : "not configured, password reset emails are not sent");
        log.info("===================================");
    }
}
