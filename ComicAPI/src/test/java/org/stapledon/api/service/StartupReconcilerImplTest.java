package org.stapledon.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stapledon.config.JsonConfigWriter;
import org.stapledon.config.TaskExecutionTracker;
import org.stapledon.config.properties.StartupReconcilerProperties;
import org.stapledon.downloader.ComicCacher;
import org.stapledon.dto.ComicConfig;
import org.stapledon.dto.ComicItem;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StartupReconcilerImplTest {

    @Mock
    private StartupReconcilerProperties properties;

    @Mock
    private JsonConfigWriter jsonConfigWriter;

    @Mock
    private ComicCacher comicCacher;

    @Mock
    private TaskExecutionTracker taskExecutionTracker;

    private StartupReconcilerImpl startupReconciler;

    @BeforeEach
    void setUp() {
        startupReconciler = new StartupReconcilerImpl(properties, jsonConfigWriter, comicCacher, taskExecutionTracker);
    }

    @Test
    void runShouldScheduleReconciliationWhenEnabled() throws Exception {
        // Given
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getScheduleTime()).thenReturn("06:00:00");
        when(jsonConfigWriter.loadComics()).thenReturn(new ComicConfig());

        // When
        startupReconciler.run();

        // Then
        verify(properties).isEnabled();
        verify(properties).getScheduleTime();
        verify(jsonConfigWriter).loadComics();
    }

    @Test
    void runShouldNotScheduleReconciliationWhenDisabled() throws Exception {
        // Given
        when(properties.isEnabled()).thenReturn(false);

        // When
        startupReconciler.run();

        // Then
        verify(properties).isEnabled();
        verifyNoMoreInteractions(jsonConfigWriter, comicCacher, taskExecutionTracker);
    }

    @Test
    void reconcileShouldRunReconciliationWhenNotRunToday() throws IOException {
        // Given
        ComicConfig comicConfig = new ComicConfig();
        comicConfig.setItems(new HashMap<>());

        when(jsonConfigWriter.loadComics()).thenReturn(comicConfig);
        when(taskExecutionTracker.canRunToday("StartupReconciler")).thenReturn(true);
        when(comicCacher.bootstrapConfig()).thenReturn(mock(org.stapledon.dto.Bootstrap.class));

        // When
        boolean result = startupReconciler.reconcile();

        // Then
        assertTrue(result);
        verify(taskExecutionTracker).canRunToday("StartupReconciler");
        verify(comicCacher).bootstrapConfig();
        verify(taskExecutionTracker).markTaskExecuted("StartupReconciler");
    }

    @Test
    void reconcileShouldSkipReconciliationWhenAlreadyRunToday() throws IOException {
        // Given
        ComicConfig comicConfig = new ComicConfig();
        comicConfig.setItems(new HashMap<>());

        when(jsonConfigWriter.loadComics()).thenReturn(comicConfig);
        when(taskExecutionTracker.canRunToday("StartupReconciler")).thenReturn(false);
        when(taskExecutionTracker.getLastExecutionDate("StartupReconciler")).thenReturn(LocalDate.now());

        // When
        boolean result = startupReconciler.reconcile();

        // Then
        assertTrue(result);
        verify(taskExecutionTracker).canRunToday("StartupReconciler");
        verify(taskExecutionTracker).getLastExecutionDate("StartupReconciler");
        verify(comicCacher, never()).bootstrapConfig();
        verify(taskExecutionTracker, never()).markTaskExecuted("StartupReconciler");
    }

    @Test
    void scheduleReconciliationShouldSetUpScheduler() {
        // Given
        when(properties.getScheduleTime()).thenReturn("06:00:00");
        when(taskExecutionTracker.canRunToday("StartupReconciler")).thenReturn(true);

        // When
        startupReconciler.scheduleReconciliation();

        // Then
        verify(properties).getScheduleTime();
        verify(taskExecutionTracker).canRunToday("StartupReconciler");
    }

    @Test
    void scheduleReconciliationShouldAdjustForAlreadyRunToday() {
        // Given
        when(properties.getScheduleTime()).thenReturn("06:00:00");
        when(taskExecutionTracker.canRunToday("StartupReconciler")).thenReturn(false);
        when(taskExecutionTracker.getLastExecutionDate("StartupReconciler")).thenReturn(LocalDate.now());

        // When
        startupReconciler.scheduleReconciliation();

        // Then
        verify(properties).getScheduleTime();
        verify(taskExecutionTracker).canRunToday("StartupReconciler");
        verify(taskExecutionTracker).getLastExecutionDate("StartupReconciler");
    }
}