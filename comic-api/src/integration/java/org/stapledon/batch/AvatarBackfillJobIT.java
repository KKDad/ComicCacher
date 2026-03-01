package org.stapledon.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.TestPropertySource;
import org.stapledon.common.dto.ComicConfig;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ImageFormat;
import org.stapledon.common.service.ComicConfigurationService;
import org.stapledon.engine.management.ManagementFacade;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;

/**
 * Integration tests for AvatarBackfillJob.
 * Tests the complete flow of downloading missing avatar images.
 */
@Slf4j
@TestPropertySource(properties = {
        "batch.avatar-backfill.enabled=true",
        "batch.avatar-backfill.cron=0 15 7 * * ?",
        "batch.avatar-backfill.delay-between-downloads-ms=0"
})
class AvatarBackfillJobIT extends AbstractBatchJobIntegrationTest {

    @Autowired private JobOperator jobOperator;
    @Autowired @Qualifier("avatarBackfillJob") private Job avatarBackfillJob;
    @Autowired private ManagementFacade managementFacade;
    @Autowired private ComicConfigurationService comicConfigurationService;

    private static final int TEST_COMIC_ID = 99999;
    private static final String TEST_COMIC_NAME = "TestAvatarComic";

    private JobExecution runJob() throws Exception {
        var params = new JobParametersBuilder()
                .addString("runId", String.valueOf(System.currentTimeMillis()))
                .addString("trigger", "TEST")
                .toJobParameters();

        return jobOperator.start(avatarBackfillJob, params);
    }

    @BeforeEach
    void setupTestComics() throws Exception {
        log.info("Setting up test comics for AvatarBackfillJob tests");

        // Clean up any avatar file from previous tests
        String comicDirName = TEST_COMIC_NAME.replace(" ", "");
        Path avatarPath = Paths.get(BATCH_CACHE_DIR, comicDirName, "avatar.png");
        Files.deleteIfExists(avatarPath);

        // Create a comic config with a test comic that has avatarAvailable=false
        ComicConfig config = new ComicConfig();
        config.setItems(new HashMap<>());

        ComicItem testComic = ComicItem.builder()
                .id(TEST_COMIC_ID)
                .name(TEST_COMIC_NAME)
                .author("Test Author")
                .enabled(true)
                .avatarAvailable(false)
                .source("gocomics")
                .sourceIdentifier("testavatarcomic")
                .oldest(LocalDate.now().minusDays(30))
                .newest(LocalDate.now())
                .build();

        config.getItems().put(TEST_COMIC_ID, testComic);
        comicConfigurationService.saveComicConfig(config);
        managementFacade.refreshComicList();
    }

    /**
     * Test: Job completes successfully even when no avatars can be downloaded
     * (no real downloader strategies in test environment).
     */
    @Test
    void avatarBackfillJobCompletesSuccessfully() throws Exception {
        log.info("TEST: AvatarBackfillJob completes successfully");

        JobExecution jobExecution = runJob();

        assertThat(jobExecution).as("JobExecution should not be null").isNotNull();
        assertThat(jobExecution.getStatus())
                .as("Job should complete successfully")
                .isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getExitStatus().getExitCode())
                .as("Exit code should be COMPLETED")
                .isEqualTo("COMPLETED");

        assertBatchExecutionTracked("AvatarBackfillJob");
        assertBatchExecutionValid("AvatarBackfillJob", "COMPLETED");

        log.info("SUCCESS: AvatarBackfillJob completed");
    }

    /**
     * Test: When avatar already exists on disk, refreshComicList syncs the
     * avatarAvailable flag to true without needing the download job.
     */
    @Test
    void refreshComicListSyncsAvatarAvailableFlag() throws Exception {
        log.info("TEST: refreshComicList syncs avatarAvailable flag");

        // Verify comic starts with avatarAvailable=false
        var comicBefore = managementFacade.getComic(TEST_COMIC_ID);
        assertThat(comicBefore).isPresent();
        assertThat(comicBefore.get().isAvatarAvailable())
                .as("avatarAvailable should start as false")
                .isFalse();

        // Place an avatar file on disk at the expected location
        String comicDirName = TEST_COMIC_NAME.replace(" ", "");
        Path avatarPath = Paths.get(BATCH_CACHE_DIR, comicDirName, "avatar.png");
        Files.createDirectories(avatarPath.getParent());
        File avatarFile = createTestImage(comicDirName + "/avatar.png", 100, 100, ImageFormat.PNG);
        assertThat(avatarFile.exists()).as("Avatar file should exist on disk").isTrue();

        // Refresh comic list — should detect the avatar and update the flag
        managementFacade.refreshComicList();

        // Verify avatarAvailable is now true
        var comicAfter = managementFacade.getComic(TEST_COMIC_ID);
        assertThat(comicAfter).isPresent();
        assertThat(comicAfter.get().isAvatarAvailable())
                .as("avatarAvailable should be synced to true after refresh")
                .isTrue();

        log.info("SUCCESS: avatarAvailable flag synced correctly");
    }

    /**
     * Test: Job skips comics that already have avatars on disk.
     */
    @Test
    void avatarBackfillJobSkipsExistingAvatars() throws Exception {
        log.info("TEST: AvatarBackfillJob skips comics with existing avatars");

        // Place an avatar file on disk
        String comicDirName = TEST_COMIC_NAME.replace(" ", "");
        createTestImage(comicDirName + "/avatar.png", 100, 100, ImageFormat.PNG);

        // Refresh to sync the flag
        managementFacade.refreshComicList();

        // Run the job — should skip this comic since avatar exists
        JobExecution jobExecution = runJob();

        assertThat(jobExecution.getStatus())
                .as("Job should complete successfully")
                .isEqualTo(BatchStatus.COMPLETED);

        // Avatar should still exist and flag should still be true
        var comic = managementFacade.getComic(TEST_COMIC_ID);
        assertThat(comic).isPresent();
        assertThat(comic.get().isAvatarAvailable())
                .as("avatarAvailable should remain true")
                .isTrue();

        log.info("SUCCESS: Job skipped comic with existing avatar");
    }

    /**
     * Test: Job is idempotent — running twice produces the same result.
     */
    @Test
    void avatarBackfillJobIsIdempotent() throws Exception {
        log.info("TEST: AvatarBackfillJob idempotency");

        // Run job twice
        JobExecution execution1 = runJob();
        assertThat(execution1.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        JobExecution execution2 = runJob();
        assertThat(execution2.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        log.info("SUCCESS: Job is idempotent");
    }
}
