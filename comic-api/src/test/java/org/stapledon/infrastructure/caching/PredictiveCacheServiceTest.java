package org.stapledon.infrastructure.caching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stapledon.common.config.CaffeineCacheProperties;
import org.stapledon.common.dto.ComicNavigationResult;
import org.stapledon.common.dto.ImageDto;
import org.stapledon.common.util.Direction;
import org.stapledon.engine.management.ComicManagementFacade;

/**
 * Unit tests for PredictiveCacheService.
 * Note: Since PredictiveCacheService uses @Async, we test that methods execute without errors
 * rather than verifying async behavior which would require integration testing.
 */
@ExtendWith(MockitoExtension.class)
class PredictiveCacheServiceTest {

    @Mock
    private ComicManagementFacade comicManagementFacade;

    private CaffeineCacheProperties cacheProperties;
    private PredictiveCacheService service;

    @BeforeEach
    void setUp() {
        cacheProperties = new CaffeineCacheProperties();
        cacheProperties.setEnabled(true);

        CaffeineCacheProperties.LookaheadConfig lookahead = new CaffeineCacheProperties.LookaheadConfig();
        lookahead.setEnabled(true);
        lookahead.setCount(3);
        cacheProperties.setLookahead(lookahead);

        service = new PredictiveCacheService(comicManagementFacade, cacheProperties);
    }

    @Test
    void testServiceCreation() {
        assertNotNull(service, "Service should be created successfully");
    }

    @Test
    void testPrefetchAdjacentComics_Forward_CallsCorrectDates() {
        int comicId = 1;
        LocalDate startDate = LocalDate.of(2025, 10, 24);

        // Mock successful navigation results - each call returns the next date
        ImageDto mockImage = ImageDto.builder()
            .imageData("mock-data")
            .mimeType("image/png")
            .build();

        when(comicManagementFacade.getComicStrip(eq(comicId), eq(Direction.FORWARD), any(LocalDate.class)))
            .thenReturn(
                ComicNavigationResult.builder()
                    .found(true)
                    .image(mockImage)
                    .currentDate(startDate.plusDays(1))
                    .nearestPreviousDate(startDate)
                    .nearestNextDate(startDate.plusDays(2))
                    .build(),
                ComicNavigationResult.builder()
                    .found(true)
                    .image(mockImage)
                    .currentDate(startDate.plusDays(2))
                    .nearestPreviousDate(startDate.plusDays(1))
                    .nearestNextDate(startDate.plusDays(3))
                    .build(),
                ComicNavigationResult.builder()
                    .found(true)
                    .image(mockImage)
                    .currentDate(startDate.plusDays(3))
                    .nearestPreviousDate(startDate.plusDays(2))
                    .nearestNextDate(null)
                    .build()
            );

        // Execute prefetch
        service.prefetchAdjacentComics(comicId, startDate, Direction.FORWARD);

        // Capture the arguments
        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(comicManagementFacade, atLeast(3))
            .getComicStrip(eq(comicId), eq(Direction.FORWARD), dateCaptor.capture());

        // Verify dates are sequential
        List<LocalDate> capturedDates = dateCaptor.getAllValues();
        assertTrue(capturedDates.size() >= 3, "Should have fetched at least 3 comics");

        // First call should be from the start date
        assertEquals(startDate, capturedDates.get(0), "First fetch should be from start date");

        // Second call should be from the next date (startDate + 1)
        assertEquals(startDate.plusDays(1), capturedDates.get(1), "Second fetch should be from next date");

        // Third call should be from the date after that (startDate + 2)
        assertEquals(startDate.plusDays(2), capturedDates.get(2), "Third fetch should be from date after that");
    }

    @Test
    void testPrefetchAdjacentComics_Backward_CallsCorrectDates() {
        int comicId = 1;
        LocalDate startDate = LocalDate.of(2025, 10, 24);

        // Mock successful navigation results going backward
        ImageDto mockImage = ImageDto.builder()
            .imageData("mock-data")
            .mimeType("image/png")
            .build();

        when(comicManagementFacade.getComicStrip(eq(comicId), eq(Direction.BACKWARD), any(LocalDate.class)))
            .thenReturn(
                ComicNavigationResult.builder()
                    .found(true)
                    .image(mockImage)
                    .currentDate(startDate.minusDays(1))
                    .nearestPreviousDate(startDate.minusDays(2))
                    .nearestNextDate(startDate)
                    .build(),
                ComicNavigationResult.builder()
                    .found(true)
                    .image(mockImage)
                    .currentDate(startDate.minusDays(2))
                    .nearestPreviousDate(startDate.minusDays(3))
                    .nearestNextDate(startDate.minusDays(1))
                    .build(),
                ComicNavigationResult.builder()
                    .found(true)
                    .image(mockImage)
                    .currentDate(startDate.minusDays(3))
                    .nearestPreviousDate(null)
                    .nearestNextDate(startDate.minusDays(2))
                    .build()
            );

        // Execute prefetch
        service.prefetchAdjacentComics(comicId, startDate, Direction.BACKWARD);

        // Capture the arguments
        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(comicManagementFacade, atLeast(3))
            .getComicStrip(eq(comicId), eq(Direction.BACKWARD), dateCaptor.capture());

        // Verify dates are going backward
        List<LocalDate> capturedDates = dateCaptor.getAllValues();
        assertTrue(capturedDates.size() >= 3, "Should have fetched at least 3 comics");

        // Verify backward progression
        assertEquals(startDate, capturedDates.get(0), "First fetch should be from start date");
        assertEquals(startDate.minusDays(1), capturedDates.get(1), "Second fetch should be from previous date");
        assertEquals(startDate.minusDays(2), capturedDates.get(2), "Third fetch should be from date before that");
    }

    @Test
    void testPrefetchAdjacentComics_DisabledLookahead() {
        // Disable lookahead
        cacheProperties.getLookahead().setEnabled(false);

        int comicId = 1;
        LocalDate startDate = LocalDate.of(2025, 10, 24);

        // Execute prefetch
        service.prefetchAdjacentComics(comicId, startDate, Direction.FORWARD);

        // Verify no calls were made when lookahead is disabled
        verify(comicManagementFacade, never())
            .getComicStrip(any(Integer.class), any(Direction.class), any(LocalDate.class));
    }

    @Test
    void testPrefetchAdjacentComics_StopsAtEnd() {
        int comicId = 1;
        LocalDate startDate = LocalDate.of(2025, 10, 24);

        // Mock successful first result, then hit end
        ImageDto mockImage = ImageDto.builder()
            .imageData("mock-data")
            .mimeType("image/png")
            .build();

        when(comicManagementFacade.getComicStrip(eq(comicId), eq(Direction.FORWARD), any(LocalDate.class)))
            .thenReturn(
                ComicNavigationResult.builder()
                    .found(true)
                    .image(mockImage)
                    .currentDate(startDate.plusDays(1))
                    .nearestPreviousDate(startDate)
                    .nearestNextDate(null)
                    .build(),
                ComicNavigationResult.builder()
                    .found(false)
                    .reason("AT_END")
                    .requestedDate(startDate.plusDays(1))
                    .nearestPreviousDate(startDate.plusDays(1))
                    .nearestNextDate(null)
                    .build()
            );

        // Execute prefetch
        service.prefetchAdjacentComics(comicId, startDate, Direction.FORWARD);

        // Capture the arguments
        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(comicManagementFacade, atLeast(1))
            .getComicStrip(eq(comicId), eq(Direction.FORWARD), dateCaptor.capture());

        // Should have stopped after hitting the end (no more than 2 calls)
        List<LocalDate> capturedDates = dateCaptor.getAllValues();
        assertTrue(capturedDates.size() <= 2,
            "Should have stopped after hitting end, found " + capturedDates.size() + " calls");
    }

    @Test
    void testLookaheadCountIsRespected() {
        // Set lookahead count to 2 instead of default 3
        cacheProperties.getLookahead().setCount(2);

        int comicId = 1;
        LocalDate startDate = LocalDate.of(2025, 10, 24);

        // Mock successful navigation results
        ImageDto mockImage = ImageDto.builder()
            .imageData("mock-data")
            .mimeType("image/png")
            .build();

        when(comicManagementFacade.getComicStrip(eq(comicId), eq(Direction.FORWARD), any(LocalDate.class)))
            .thenReturn(
                ComicNavigationResult.builder()
                    .found(true)
                    .image(mockImage)
                    .currentDate(startDate.plusDays(1))
                    .build(),
                ComicNavigationResult.builder()
                    .found(true)
                    .image(mockImage)
                    .currentDate(startDate.plusDays(2))
                    .build()
            );

        // Execute prefetch
        service.prefetchAdjacentComics(comicId, startDate, Direction.FORWARD);

        // Capture the arguments
        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(comicManagementFacade, atLeast(2))
            .getComicStrip(eq(comicId), eq(Direction.FORWARD), dateCaptor.capture());

        // Should have fetched exactly 2 comics (respecting the count)
        List<LocalDate> capturedDates = dateCaptor.getAllValues();
        assertEquals(2, capturedDates.size(),
            "Should have fetched exactly 2 comics when lookahead count is 2");
    }
}
