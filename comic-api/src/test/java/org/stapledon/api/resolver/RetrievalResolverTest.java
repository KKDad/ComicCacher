package org.stapledon.api.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stapledon.common.dto.ComicRetrievalRecord;
import org.stapledon.common.dto.ComicRetrievalStatus;
import org.stapledon.common.service.RetrievalStatusService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Tests for RetrievalResolver.
 */
@ExtendWith(MockitoExtension.class)
class RetrievalResolverTest {

    @Mock
    private RetrievalStatusService retrievalStatusService;

    private RetrievalResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new RetrievalResolver(retrievalStatusService);
    }

    // =========================================================================
    // retrievalRecords
    // =========================================================================

    @Test
    void retrievalRecordsReturnsEmptyListWhenNoRecords() {
        when(retrievalStatusService.getRetrievalRecords(isNull(), isNull(), isNull(), isNull(), eq(100)))
                .thenReturn(List.of());

        List<RetrievalResolver.RetrievalRecordDto> result = resolver.retrievalRecords(null, null, null, null, null);

        assertThat(result).isEmpty();
    }

    @Test
    void retrievalRecordsMapsFieldsCorrectly() {
        var record = ComicRetrievalRecord.success(
                "Garfield", LocalDate.of(2024, 1, 15), "gocomics", 150L, 52428L);

        when(retrievalStatusService.getRetrievalRecords(isNull(), isNull(), isNull(), isNull(), eq(100)))
                .thenReturn(List.of(record));

        List<RetrievalResolver.RetrievalRecordDto> result = resolver.retrievalRecords(null, null, null, null, null);

        assertThat(result).hasSize(1);
        RetrievalResolver.RetrievalRecordDto mapped = result.getFirst();
        assertThat(mapped.comicName()).isEqualTo("Garfield");
        assertThat(mapped.comicDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(mapped.source()).isEqualTo("gocomics");
        assertThat(mapped.status()).isEqualTo(ComicRetrievalStatus.SUCCESS);
        assertThat(mapped.retrievalDurationMs()).isEqualTo(150.0);
        assertThat(mapped.imageSize()).isEqualTo(52428.0);
    }

    @Test
    void retrievalRecordsPassesLimitToService() {
        when(retrievalStatusService.getRetrievalRecords(isNull(), isNull(), isNull(), isNull(), eq(50)))
                .thenReturn(List.of());

        resolver.retrievalRecords(null, null, null, null, 50);

        verify(retrievalStatusService).getRetrievalRecords(isNull(), isNull(), isNull(), isNull(), eq(50));
    }

    @Test
    void retrievalRecordsDefaultsLimitTo100() {
        when(retrievalStatusService.getRetrievalRecords(isNull(), isNull(), isNull(), isNull(), eq(100)))
                .thenReturn(List.of());

        resolver.retrievalRecords(null, null, null, null, null);

        verify(retrievalStatusService).getRetrievalRecords(isNull(), isNull(), isNull(), isNull(), eq(100));
    }

    // =========================================================================
    // retrievalRecord
    // =========================================================================

    @Test
    void retrievalRecordReturnsNullWhenNotFound() {
        when(retrievalStatusService.getRetrievalRecord("nonexistent")).thenReturn(Optional.empty());

        RetrievalResolver.RetrievalRecordDto result = resolver.retrievalRecord("nonexistent");

        assertThat(result).isNull();
    }

    @Test
    void retrievalRecordReturnsMappedRecord() {
        var record = ComicRetrievalRecord.success(
                "Peanuts", LocalDate.of(2024, 3, 10), "gocomics", 200L, 30000L);

        when(retrievalStatusService.getRetrievalRecord("Peanuts_2024-03-10")).thenReturn(Optional.of(record));

        RetrievalResolver.RetrievalRecordDto result = resolver.retrievalRecord("Peanuts_2024-03-10");

        assertThat(result).isNotNull();
        assertThat(result.comicName()).isEqualTo("Peanuts");
        assertThat(result.status()).isEqualTo(ComicRetrievalStatus.SUCCESS);
    }

    // =========================================================================
    // retrievalRecordsForComic
    // =========================================================================

    @Test
    void retrievalRecordsForComicDefaultsLimitTo20() {
        when(retrievalStatusService.getRetrievalRecords(eq("Garfield"), isNull(), isNull(), isNull(), eq(20)))
                .thenReturn(List.of());

        resolver.retrievalRecordsForComic("Garfield", null);

        verify(retrievalStatusService).getRetrievalRecords(eq("Garfield"), isNull(), isNull(), isNull(), eq(20));
    }

    // =========================================================================
    // retrievalSummary
    // =========================================================================

    @Test
    void retrievalSummaryBuildsSummaryFromServiceData() {
        Map<ComicRetrievalStatus, Long> countsByStatus = Map.of(
                ComicRetrievalStatus.SUCCESS, 90L,
                ComicRetrievalStatus.NETWORK_ERROR, 10L);

        Map<String, Object> rawSummary = Map.of(
                "totalCount", 100,
                "countsByStatus", countsByStatus,
                "successRate", 0.9,
                "averageDurationMillis", 250.0,
                "comicsWithMostFailures", Map.of("Garfield", 5L));

        when(retrievalStatusService.getRetrievalSummary(isNull(), isNull())).thenReturn(rawSummary);

        RetrievalResolver.RetrievalSummaryDto result = resolver.retrievalSummary(null, null);

        assertThat(result.totalAttempts()).isEqualTo(100);
        assertThat(result.successCount()).isEqualTo(90);
        assertThat(result.successRate()).isEqualTo(90.0);
        assertThat(result.averageDurationMs()).isEqualTo(250.0);
    }

    @Test
    void retrievalSummaryHandlesEmptyData() {
        Map<String, Object> rawSummary = Map.of(
                "totalCount", 0,
                "countsByStatus", Map.of(),
                "successRate", 0.0,
                "averageDurationMillis", 0.0,
                "comicsWithMostFailures", Map.of());

        when(retrievalStatusService.getRetrievalSummary(isNull(), isNull())).thenReturn(rawSummary);

        RetrievalResolver.RetrievalSummaryDto result = resolver.retrievalSummary(null, null);

        assertThat(result.totalAttempts()).isEqualTo(0);
        assertThat(result.successCount()).isEqualTo(0);
        assertThat(result.averageDurationMs()).isNull();
    }

    // =========================================================================
    // Status enum mapping (now 1:1 — no lossy mapping)
    // =========================================================================

    record StatusMappingCase(String label, ComicRetrievalStatus javaStatus, ComicRetrievalStatus expectedGraphql) {
    }

    static Stream<StatusMappingCase> statusMappingCases() {
        return Stream.of(
                new StatusMappingCase("SUCCESS maps to SUCCESS", ComicRetrievalStatus.SUCCESS, ComicRetrievalStatus.SUCCESS),
                new StatusMappingCase("NETWORK_ERROR maps to NETWORK_ERROR", ComicRetrievalStatus.NETWORK_ERROR, ComicRetrievalStatus.NETWORK_ERROR),
                new StatusMappingCase("PARSING_ERROR maps to PARSING_ERROR", ComicRetrievalStatus.PARSING_ERROR, ComicRetrievalStatus.PARSING_ERROR),
                new StatusMappingCase("COMIC_UNAVAILABLE maps to COMIC_UNAVAILABLE", ComicRetrievalStatus.COMIC_UNAVAILABLE, ComicRetrievalStatus.COMIC_UNAVAILABLE),
                new StatusMappingCase("AUTHENTICATION_ERROR maps to AUTHENTICATION_ERROR", ComicRetrievalStatus.AUTHENTICATION_ERROR, ComicRetrievalStatus.AUTHENTICATION_ERROR),
                new StatusMappingCase("STORAGE_ERROR maps to STORAGE_ERROR", ComicRetrievalStatus.STORAGE_ERROR, ComicRetrievalStatus.STORAGE_ERROR),
                new StatusMappingCase("UNKNOWN_ERROR maps to UNKNOWN_ERROR", ComicRetrievalStatus.UNKNOWN_ERROR, ComicRetrievalStatus.UNKNOWN_ERROR)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("statusMappingCases")
    void statusMapping(StatusMappingCase tc) {
        var record = ComicRetrievalRecord.builder()
                .id("test_2024-01-01")
                .comicName("test")
                .comicDate(LocalDate.of(2024, 1, 1))
                .status(tc.javaStatus)
                .build();

        when(retrievalStatusService.getRetrievalRecords(isNull(), isNull(), isNull(), isNull(), eq(100)))
                .thenReturn(List.of(record));

        List<RetrievalResolver.RetrievalRecordDto> result = resolver.retrievalRecords(null, null, null, null, null);

        assertThat(result.getFirst().status()).isEqualTo(tc.expectedGraphql);
    }

    // =========================================================================
    // Mutations
    // =========================================================================

    @Test
    void deleteRetrievalRecordDelegatesToService() {
        when(retrievalStatusService.deleteRetrievalRecord("test_id")).thenReturn(true);

        var result = resolver.deleteRetrievalRecord("test_id");

        assertThat(result.success()).isTrue();
        verify(retrievalStatusService).deleteRetrievalRecord("test_id");
    }

    @ParameterizedTest
    @CsvSource({"7,7", ",7", "30,30"})
    void purgeRetrievalRecordsUsesCorrectDays(Integer input, int expectedDays) {
        when(retrievalStatusService.purgeOldRecords(expectedDays)).thenReturn(5);

        var result = resolver.purgeRetrievalRecords(input);

        assertThat(result.purgedCount()).isEqualTo(5);
        verify(retrievalStatusService).purgeOldRecords(expectedDays);
    }
}
