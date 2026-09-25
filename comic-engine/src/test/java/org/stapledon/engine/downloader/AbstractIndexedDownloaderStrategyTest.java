package org.stapledon.engine.downloader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.jsoup.HttpStatusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import org.stapledon.common.dto.ComicDownloadResult;
import org.stapledon.common.dto.ComicDownloadResult.FailureKind;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.infrastructure.web.InspectorService;
import org.stapledon.common.infrastructure.web.UserAgentService;
import org.stapledon.common.service.ValidationService;

@ExtendWith(MockitoExtension.class)
@DisplayName("AbstractIndexedDownloaderStrategy")
class AbstractIndexedDownloaderStrategyTest {

    @Mock
    private InspectorService webInspector;

    @Mock
    private ValidationService imageValidationService;

    @Mock
    private UserAgentService userAgentService;

    @Mock
    private SourceThrottleService throttleService;

    private TestIndexedStrategy strategy;
    private ComicItem comic;

    @BeforeEach
    void setUp() {
        strategy = new TestIndexedStrategy(webInspector, imageValidationService, userAgentService, throttleService);
        comic = new ComicItem();
        comic.setId(1);
        comic.setName("Freefall");
    }

    @Test
    @DisplayName("a 429 on a strip backs the source off and reports RATE_LIMITED")
    void rateLimitedStrip() {
        strategy.failWith(RateLimitedException.of("https://example.com/strip", "12"));
        when(throttleService.backOff(eq("indexed-source"), anyInt(), any())).thenReturn(Duration.ofSeconds(12));

        ComicDownloadResult result = strategy.downloadStrip(comic, 42);

        assertThat(result.isRateLimited()).isTrue();
        verify(throttleService).backOff("indexed-source", 1, Optional.of(Duration.ofSeconds(12)));
    }

    @Test
    @DisplayName("a 429 raised as a plain HTTP error is also RATE_LIMITED")
    void httpStatus429Strip() {
        strategy.failWith(new HttpStatusException("HTTP error fetching URL", 429, "https://example.com/strip"));
        when(throttleService.backOff(eq("indexed-source"), anyInt(), any())).thenReturn(Duration.ofSeconds(30));

        ComicDownloadResult result = strategy.downloadStrip(comic, 42);

        assertThat(result.isRateLimited()).isTrue();
        verify(throttleService).backOff("indexed-source", 1, Optional.empty());
    }

    @Test
    @DisplayName("a 404 on a strip is UNAVAILABLE, with no back-off")
    void missingStrip() {
        strategy.failWith(new HttpStatusException("HTTP error fetching URL", 404, "https://example.com/strip"));

        ComicDownloadResult result = strategy.downloadStrip(comic, 42);

        assertThat(result.getFailureKind()).isEqualTo(FailureKind.UNAVAILABLE);
        verify(throttleService, never()).backOff(any(), anyInt(), any());
    }

    @Test
    @DisplayName("any other exception is ERROR")
    void otherError() {
        strategy.failWith(new IllegalStateException("parse failed"));

        ComicDownloadResult result = strategy.downloadStrip(comic, 42);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getFailureKind()).isEqualTo(FailureKind.ERROR);
    }

    private static final class TestIndexedStrategy extends AbstractIndexedDownloaderStrategy {
        private Exception failure;

        TestIndexedStrategy(InspectorService webInspector, ValidationService imageValidationService, UserAgentService userAgentService,
                SourceThrottleService throttleService) {
            super("indexed-source", webInspector, imageValidationService, userAgentService, throttleService);
        }

        void failWith(Exception failure) {
            this.failure = failure;
        }

        @Override
        protected IndexedStripData fetchLatestStrip(ComicItem comic) throws Exception {
            throw failure;
        }

        @Override
        protected IndexedStripData fetchStrip(ComicItem comic, int stripNumber) throws Exception {
            throw failure;
        }

        @Override
        protected byte[] downloadAvatarImage(int comicId, String comicName, String sourceIdentifier) {
            return new byte[0];
        }
    }
}
