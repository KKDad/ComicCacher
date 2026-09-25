package org.stapledon.engine.downloader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpServer;
import org.jsoup.HttpStatusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;


import org.stapledon.common.dto.ComicDownloadRequest;
import org.stapledon.common.dto.ComicDownloadResult;
import org.stapledon.common.dto.ImageFormat;
import org.stapledon.common.dto.ImageValidationResult;
import org.stapledon.common.infrastructure.web.InspectorService;
import org.stapledon.common.infrastructure.web.UserAgentService;
import org.stapledon.common.service.ValidationService;

@ExtendWith(MockitoExtension.class)
class AbstractComicDownloaderStrategyTest {

    @Mock
    private InspectorService webInspector;

    @Mock
    private ValidationService imageValidationService;

    @Mock
    private UserAgentService userAgentService;

    @Mock
    private SourceThrottleService throttleService;

    private TestComicDownloaderStrategy strategy;
    private final byte[] validImageData = "valid-image-data".getBytes();
    private final byte[] emptyImageData = new byte[0];

    @BeforeEach
    void setUp() {
        strategy = new TestComicDownloaderStrategy("test-source", webInspector, imageValidationService, userAgentService, throttleService);
    }

    @Test
    void shouldDownloadComicSuccessfully() {
        // Arrange
        ComicDownloadRequest request = ComicDownloadRequest.builder()
                .comicId(1)
                .comicName("Test Comic")
                .source("test-source")
                .sourceIdentifier("test-comic")
                .date(LocalDate.now())
                .build();

        ImageValidationResult validationResult = ImageValidationResult.success(
                ImageFormat.PNG, 800, 600, validImageData.length);

        strategy.setMockImageData(validImageData);
        when(imageValidationService.validate(validImageData)).thenReturn(validationResult);

        // Act
        ComicDownloadResult result = strategy.downloadComic(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getImageData()).isNotNull();
        assertThat(result.getImageData()).isEqualTo(validImageData);
        assertThat(result.getRequest()).isNotNull();
        assertThat(result.getRequest()).isEqualTo(request);
        assertThat(validationResult.getFormat()).isEqualTo(ImageFormat.PNG);
        assertThat(validationResult.getWidth()).isEqualTo(800);
        assertThat(validationResult.getHeight()).isEqualTo(600);
    }

    @Test
    void shouldFailWhenImageDataIsNull() {
        // Arrange
        ComicDownloadRequest request = ComicDownloadRequest.builder()
                .comicId(1)
                .comicName("Test Comic")
                .source("test-source")
                .sourceIdentifier("test-comic")
                .date(LocalDate.now())
                .build();

        strategy.setMockImageData(null);

        // Act
        ComicDownloadResult result = strategy.downloadComic(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getErrorMessage()).isNotNull();
        assertThat(result.getErrorMessage().contains("empty")).isTrue();
        assertThat(result.getRequest()).isNotNull();
        assertThat(result.getRequest()).isEqualTo(request);
    }

    @Test
    void shouldFailWhenImageDataIsEmpty() {
        // Arrange
        ComicDownloadRequest request = ComicDownloadRequest.builder()
                .comicId(1)
                .comicName("Test Comic")
                .source("test-source")
                .sourceIdentifier("test-comic")
                .date(LocalDate.now())
                .build();

        strategy.setMockImageData(emptyImageData);

        // Act
        ComicDownloadResult result = strategy.downloadComic(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getErrorMessage()).isNotNull();
        assertThat(result.getErrorMessage().contains("empty")).isTrue();
        assertThat(result.getRequest()).isNotNull();
        assertThat(result.getRequest()).isEqualTo(request);
    }

    @Test
    void shouldFailWhenImageValidationFails() {
        // Arrange
        ComicDownloadRequest request = ComicDownloadRequest.builder()
                .comicId(1)
                .comicName("Test Comic")
                .source("test-source")
                .sourceIdentifier("test-comic")
                .date(LocalDate.now())
                .build();

        ImageValidationResult validationResult = ImageValidationResult.failure(
                "Image is corrupted");

        strategy.setMockImageData(validImageData);
        when(imageValidationService.validate(validImageData)).thenReturn(validationResult);

        // Act
        ComicDownloadResult result = strategy.downloadComic(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getErrorMessage().contains("Invalid image")).isTrue();
        assertThat(result.getErrorMessage().contains("corrupted")).isTrue();
    }

    @Test
    void shouldFailWhenDownloadThrowsException() {
        // Arrange
        ComicDownloadRequest request = ComicDownloadRequest.builder()
                .comicId(1)
                .comicName("Test Comic")
                .source("test-source")
                .sourceIdentifier("test-comic")
                .date(LocalDate.now())
                .build();

        strategy.setThrowException(true);

        // Act
        ComicDownloadResult result = strategy.downloadComic(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getErrorMessage()).isNotNull();
        assertThat(result.getErrorMessage().contains("Error downloading comic")).isTrue();
        assertThat(result.getRequest()).isNotNull();
        assertThat(result.getRequest()).isEqualTo(request);
    }

    @Test
    void shouldDownloadAvatarSuccessfully() {
        // Arrange
        int comicId = 1;
        String comicName = "Test Comic";
        String sourceIdentifier = "test-comic";

        ImageValidationResult validationResult = ImageValidationResult.success(
                ImageFormat.PNG, 100, 100, validImageData.length);

        strategy.setMockAvatarData(validImageData);
        when(imageValidationService.validate(validImageData)).thenReturn(validationResult);

        // Act
        Optional<byte[]> result = strategy.downloadAvatar(comicId, comicName, sourceIdentifier);

        // Assert
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get()).isEqualTo(validImageData);
    }

    @Test
    void shouldReturnEmptyWhenAvatarDataIsNull() {
        // Arrange
        int comicId = 1;
        String comicName = "Test Comic";
        String sourceIdentifier = "test-comic";

        strategy.setMockAvatarData(null);

        // Act
        Optional<byte[]> result = strategy.downloadAvatar(comicId, comicName, sourceIdentifier);

        // Assert
        assertThat(result.isPresent()).isFalse();
    }

    @Test
    void shouldReturnEmptyWhenAvatarDataIsEmpty() {
        // Arrange
        int comicId = 1;
        String comicName = "Test Comic";
        String sourceIdentifier = "test-comic";

        strategy.setMockAvatarData(emptyImageData);

        // Act
        Optional<byte[]> result = strategy.downloadAvatar(comicId, comicName, sourceIdentifier);

        // Assert
        assertThat(result.isPresent()).isFalse();
    }

    @Test
    void shouldReturnEmptyWhenAvatarValidationFails() {
        // Arrange
        int comicId = 1;
        String comicName = "Test Comic";
        String sourceIdentifier = "test-comic";

        ImageValidationResult validationResult = ImageValidationResult.failure(
                "Invalid avatar format");

        strategy.setMockAvatarData(validImageData);
        when(imageValidationService.validate(validImageData)).thenReturn(validationResult);

        // Act
        Optional<byte[]> result = strategy.downloadAvatar(comicId, comicName, sourceIdentifier);

        // Assert
        assertThat(result.isPresent()).isFalse();
    }

    @Test
    void shouldReturnEmptyWhenAvatarDownloadThrowsException() {
        // Arrange
        int comicId = 1;
        String comicName = "Test Comic";
        String sourceIdentifier = "test-comic";

        strategy.setThrowException(true);

        // Act
        Optional<byte[]> result = strategy.downloadAvatar(comicId, comicName, sourceIdentifier);

        // Assert
        assertThat(result.isPresent()).isFalse();
    }

    @Test
    void shouldRetryAfterRateLimitAndSucceed() {
        ComicDownloadRequest request = testRequest();
        ImageValidationResult validationResult = ImageValidationResult.success(
                ImageFormat.PNG, 800, 600, validImageData.length);

        strategy.setMockImageData(validImageData);
        strategy.setRateLimitsRemaining(2);
        when(throttleService.maxAttempts("test-source")).thenReturn(3);
        when(throttleService.backOff(eq("test-source"), anyInt(), any())).thenReturn(Duration.ofSeconds(7));
        when(imageValidationService.validate(validImageData)).thenReturn(validationResult);

        ComicDownloadResult result = strategy.downloadComic(request);

        assertThat(result.isSuccessful()).isTrue();
        assertThat(strategy.getDownloadCalls()).isEqualTo(3);
        verify(throttleService).backOff("test-source", 1, Optional.of(Duration.ofSeconds(7)));
        verify(throttleService).backOff("test-source", 2, Optional.of(Duration.ofSeconds(7)));
        verify(throttleService, times(3)).await("test-source");
    }

    @Test
    void shouldFailWithRateLimitMessageWhenAttemptsExhausted() {
        ComicDownloadRequest request = testRequest();

        strategy.setRateLimitsRemaining(10);
        when(throttleService.maxAttempts("test-source")).thenReturn(2);
        when(throttleService.backOff(eq("test-source"), anyInt(), any())).thenReturn(Duration.ofSeconds(7));

        ComicDownloadResult result = strategy.downloadComic(request);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getErrorMessage()).contains("Rate limited (HTTP 429)", "after 2 attempt(s)");
        assertThat(strategy.getDownloadCalls()).isEqualTo(2);
        verify(throttleService, times(1)).backOff(eq("test-source"), anyInt(), any());
    }

    @Test
    void shouldNotRetryWhenRetriesDisabled() {
        ComicDownloadRequest request = testRequest();

        strategy.setRateLimitsRemaining(1);
        when(throttleService.maxAttempts("test-source")).thenReturn(1);

        ComicDownloadResult result = strategy.downloadComic(request);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getErrorMessage()).contains("Rate limited (HTTP 429)");
        assertThat(strategy.getDownloadCalls()).isEqualTo(1);
        verify(throttleService, never()).backOff(any(), anyInt(), any());
    }

    @Test
    void shouldFailFastOnRateLimitWhenRequested() {
        ComicDownloadRequest request = testRequest().toBuilder().failFastOnRateLimit(true).build();

        strategy.setRateLimitsRemaining(10);
        when(throttleService.maxAttempts("test-source")).thenReturn(4);
        when(throttleService.backOff(eq("test-source"), anyInt(), any())).thenReturn(Duration.ofSeconds(7));

        ComicDownloadResult result = strategy.downloadComic(request);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.isRateLimited()).isTrue();
        assertThat(strategy.getDownloadCalls()).isEqualTo(1);
        // The source still backs off, so other callers slow down too
        verify(throttleService).backOff("test-source", 1, Optional.of(Duration.ofSeconds(7)));
    }

    @Test
    void shouldClassifyEmptyImageAsUnavailable() {
        strategy.setMockImageData(new byte[0]);

        ComicDownloadResult result = strategy.downloadComic(testRequest());

        assertThat(result.getFailureKind()).isEqualTo(ComicDownloadResult.FailureKind.UNAVAILABLE);
    }

    @Test
    void shouldClassifyHttp404AsUnavailable() {
        strategy.setExceptionToThrow(new HttpStatusException("HTTP error fetching URL", 404, "https://example.com/strip"));

        ComicDownloadResult result = strategy.downloadComic(testRequest());

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getFailureKind()).isEqualTo(ComicDownloadResult.FailureKind.UNAVAILABLE);
        verify(throttleService, never()).backOff(any(), anyInt(), any());
    }

    @Test
    void shouldClassifyOtherHttpErrorsAsError() {
        strategy.setExceptionToThrow(new HttpStatusException("HTTP error fetching URL", 503, "https://example.com/strip"));

        ComicDownloadResult result = strategy.downloadComic(testRequest());

        assertThat(result.getFailureKind()).isEqualTo(ComicDownloadResult.FailureKind.ERROR);
    }

    @Test
    void shouldTreatHttpStatus429AsRateLimited() {
        // Jsoup's get() reports a 429 as an HttpStatusException rather than a RateLimitedException
        strategy.setExceptionToThrow(new HttpStatusException("HTTP error fetching URL", 429, "https://example.com/strip"));
        when(throttleService.backOff(eq("test-source"), anyInt(), any())).thenReturn(Duration.ofSeconds(30));

        ComicDownloadResult result = strategy.downloadComic(testRequest());

        assertThat(result.isRateLimited()).isTrue();
        assertThat(strategy.getDownloadCalls()).isEqualTo(1);
        verify(throttleService).backOff("test-source", 1, Optional.empty());
    }

    @Test
    void downloadImageData_whenServerReturns429_throwsRateLimitedWithRetryAfter() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/img", exchange -> {
            exchange.getResponseHeaders().add("Retry-After", "42");
            exchange.sendResponseHeaders(429, -1);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/img";

            assertThatThrownBy(() -> strategy.downloadImageData(url))
                    .isInstanceOfSatisfying(RateLimitedException.class, e -> {
                        assertThat(e.getUrl()).isEqualTo(url);
                        assertThat(e.getRetryAfter()).contains(Duration.ofSeconds(42));
                    });
        } finally {
            server.stop(0);
        }
    }

    @Test
    void downloadImageData_whenServerReturns200_returnsBody() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/img", exchange -> {
            exchange.sendResponseHeaders(200, validImageData.length);
            exchange.getResponseBody().write(validImageData);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/img";

            assertThat(strategy.downloadImageData(url)).isEqualTo(validImageData);
        } finally {
            server.stop(0);
        }
    }

    private static ComicDownloadRequest testRequest() {
        return ComicDownloadRequest.builder()
                .comicId(1)
                .comicName("Test Comic")
                .source("test-source")
                .sourceIdentifier("test-comic")
                .date(LocalDate.now())
                .build();
    }

    @Test
    void shouldReturnCorrectSource() {
        // Assert
        assertThat(strategy.getSource()).isEqualTo("test-source");
    }

    /**
     * Test implementation of AbstractDailyDownloaderStrategy for testing purposes
     */
    private static class TestComicDownloaderStrategy extends AbstractDailyDownloaderStrategy {
        private byte[] mockImageData;
        private byte[] mockAvatarData;
        private boolean throwException;
        private Exception exceptionToThrow;
        private int rateLimitsRemaining;
        private int downloadCalls;

        public TestComicDownloaderStrategy(String source,
                InspectorService webInspector,
                ValidationService imageValidationService,
                UserAgentService userAgentService,
                SourceThrottleService throttleService) {
            super(source, webInspector, imageValidationService, userAgentService, throttleService);
        }

        public void setMockImageData(byte[] data) {
            this.mockImageData = data;
        }

        public void setMockAvatarData(byte[] data) {
            this.mockAvatarData = data;
        }

        public void setThrowException(boolean throwException) {
            this.throwException = throwException;
        }

        public void setExceptionToThrow(Exception exceptionToThrow) {
            this.exceptionToThrow = exceptionToThrow;
        }

        public void setRateLimitsRemaining(int rateLimitsRemaining) {
            this.rateLimitsRemaining = rateLimitsRemaining;
        }

        public int getDownloadCalls() {
            return downloadCalls;
        }

        @Override
        protected byte[] downloadComicImage(ComicDownloadRequest request) throws Exception {
            downloadCalls++;
            if (rateLimitsRemaining > 0) {
                rateLimitsRemaining--;
                throw RateLimitedException.of("https://example.com/strip", "7");
            }
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            if (throwException) {
                throw new Exception("Test exception");
            }
            return mockImageData;
        }

        @Override
        protected byte[] downloadAvatarImage(int comicId, String comicName, String sourceIdentifier) throws Exception {
            if (throwException) {
                throw new Exception("Test exception");
            }
            return mockAvatarData;
        }
    }
}
