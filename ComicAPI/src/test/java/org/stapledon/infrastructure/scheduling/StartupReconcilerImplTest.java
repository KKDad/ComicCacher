package org.stapledon.infrastructure.scheduling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stapledon.infrastructure.config.properties.StartupReconcilerProperties;
import org.stapledon.api.dto.comic.ComicConfig;
import org.stapledon.api.dto.comic.ComicItem;
import org.stapledon.core.comic.management.ComicManagementFacade;

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
    private ComicManagementFacade comicManagementFacade;

    private StartupReconcilerImpl startupReconciler;

    @BeforeEach
    void setUp() {
        startupReconciler = new StartupReconcilerImpl(properties, comicManagementFacade);
    }

    @Test
    void runShouldScheduleReconciliationWhenEnabled() throws Exception {
        // Given
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getScheduleTime()).thenReturn("06:00:00");

        // When
        startupReconciler.run();

        // Then
        verify(properties).isEnabled();
        verify(properties).getScheduleTime();
        verify(comicManagementFacade).refreshComicList();
        verify(comicManagementFacade).scheduleReconciliation("06:00:00");
    }

    @Test
    void runShouldNotScheduleReconciliationWhenDisabled() throws Exception {
        // Given
        when(properties.isEnabled()).thenReturn(false);

        // When
        startupReconciler.run();

        // Then
        verify(properties).isEnabled();
        verifyNoMoreInteractions(comicManagementFacade);
    }

    @Test
    void reconcileShouldDelegateToFacade() throws IOException {
        // Given
        when(comicManagementFacade.reconcileWithBootstrap()).thenReturn(true);

        // When
        boolean result = startupReconciler.reconcile();

        // Then
        assertTrue(result);
        verify(comicManagementFacade).reconcileWithBootstrap();
    }

    @Test
    void reconcileShouldHandleFailure() throws IOException {
        // Given
        when(comicManagementFacade.reconcileWithBootstrap()).thenReturn(false);

        // When
        boolean result = startupReconciler.reconcile();

        // Then
        assertFalse(result);
        verify(comicManagementFacade).reconcileWithBootstrap();
    }

    @Test
    void scheduleReconciliationShouldDelegateToFacade() {
        // Given
        when(properties.getScheduleTime()).thenReturn("06:00:00");

        // When
        startupReconciler.scheduleReconciliation();

        // Then
        verify(properties).getScheduleTime();
        verify(comicManagementFacade).scheduleReconciliation("06:00:00");
    }
}