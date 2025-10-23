package org.stapledon.infrastructure.scheduling;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stapledon.common.config.properties.StartupReconcilerProperties;
import org.stapledon.engine.management.ComicManagementFacade;

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
    void reconcileShouldDelegateToFacade() {
        // Given
        when(comicManagementFacade.reconcileWithBootstrap()).thenReturn(true);

        // When
        boolean result = startupReconciler.reconcile();

        // Then
        assertTrue(result);
        verify(comicManagementFacade).reconcileWithBootstrap();
    }

    @Test
    void reconcileShouldHandleFailure() {
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