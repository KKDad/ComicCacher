package org.stapledon.engine.downloader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;


import org.stapledon.common.dto.ComicDownloadRequest;
import org.stapledon.common.infrastructure.web.InspectorService;
import org.stapledon.common.infrastructure.web.UserAgentService;
import org.stapledon.common.service.ValidationService;

@ExtendWith(MockitoExtension.class)
class GoComicsDownloaderStrategyTest {

    @Mock
    private InspectorService webInspector;

    @Mock
    private ValidationService imageValidationService;

    @Mock
    private UserAgentService userAgentService;

    @Mock
    private SourceThrottleService throttleService;

    private GoComicsDownloaderStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new GoComicsDownloaderStrategy(webInspector, imageValidationService, userAgentService, throttleService);
    }

    @Test
    void shouldHaveCorrectSourceIdentifier() {
        // Assert
        assertThat(strategy.getSource()).isEqualTo("gocomics");
    }

    @Test
    void shouldCreateStrategyWithDependencies() {
        // Arrange
        InspectorService mockInspector = mock(InspectorService.class);
        ValidationService mockValidation = mock(ValidationService.class);
        UserAgentService mockUserAgent = mock(UserAgentService.class);
        SourceThrottleService mockThrottle = mock(SourceThrottleService.class);

        // Act
        GoComicsDownloaderStrategy newStrategy = new GoComicsDownloaderStrategy(
                mockInspector, mockValidation, mockUserAgent, mockThrottle);

        // Assert
        assertThat(newStrategy).isNotNull();
        assertThat(newStrategy.getSource()).isEqualTo("gocomics");
    }

    @Test
    void shouldBuildRequestCorrectly() {
        // Arrange & Act
        ComicDownloadRequest request = ComicDownloadRequest.builder()
                .comicId(1)
                .comicName("Calvin and Hobbes")
                .source("gocomics")
                .sourceIdentifier("calvinandhobbes")
                .date(LocalDate.of(2024, 1, 15))
                .build();

        // Assert
        assertThat(request).isNotNull();
        assertThat(request.getSource()).isEqualTo("gocomics");
        assertThat(request.getSourceIdentifier()).isEqualTo("calvinandhobbes");
        assertThat(request.getComicId()).isEqualTo(1);
        assertThat(request.getComicName()).isEqualTo("Calvin and Hobbes");
        assertThat(request.getDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        // Integration tests will verify actual download functionality with this strategy
        assertThat(strategy.getSource()).isEqualTo("gocomics");
    }

    @Test
    void shouldHandleSpacesInComicName() {
        // Arrange
        ComicDownloadRequest request = ComicDownloadRequest.builder()
                .comicId(1)
                .comicName("Calvin and Hobbes")
                .source("gocomics")
                .sourceIdentifier("calvinandhobbes")
                .date(LocalDate.of(2024, 1, 15))
                .build();

        // Act & Assert - verify construction doesn't throw
        assertThat(request).isNotNull();
        assertThat(request.getComicName()).isEqualTo("Calvin and Hobbes");
    }

    @Test
    void shouldFormatDateCorrectly() {
        // Arrange
        ComicDownloadRequest request = ComicDownloadRequest.builder()
                .comicId(1)
                .comicName("Test Comic")
                .source("gocomics")
                .sourceIdentifier("testcomic")
                .date(LocalDate.of(2024, 1, 15))
                .build();

        // Act & Assert
        assertThat(request.getDate()).isNotNull();
        assertThat(request.getDate().getYear()).isEqualTo(2024);
        assertThat(request.getDate().getMonthValue()).isEqualTo(1);
        assertThat(request.getDate().getDayOfMonth()).isEqualTo(15);
    }

    @Test
    void shouldHaveValidToString() {
        // Act
        String toString = strategy.toString();

        // Assert
        assertThat(toString).isNotNull();
        // Lombok's @ToString should include class name
        assertThat(toString.contains("GoComicsDownloaderStrategy")).isTrue();
    }

    /**
     * The shape of a date page's RSC payload, trimmed: the page's strip, then a related strip from another date, and an escaped JSON-LD copy
     * of the page's date inside a string.
     */
    private static final String FLIGHT = """
            0:{"a":"$@1"}
            2:["$","meta","9",{"property":"og:image","content":"https://featureassets.gocomics.com/assets/9ec074b0"}]
            5:["$","div",null,{"children":["$","$L56",null,{"comic":{"aspectRatio":3.488,"url":"https://featureassets.gocomics.com/assets/9ec074b0","isRerun":false,"id":13126094,"featureId":322,"date":"2026-10-08T00:00:00","issueDate":"2026-10-08T00:00:00","width":900}}]}]
            6:["$","script",null,{"children":"{\\\"comic\\\":{\\\"url\\\":\\\"https://example.com/ld\\\",\\\"date\\\":\\\"2026-10-07T00:00:00\\\"}}"}]
            7:["$","$L56",null,{"comic":{"aspectRatio":3.345,"url":"https://featureassets.gocomics.com/assets/239495d0","isRerun":false,"id":12861641,"featureId":322,"date":"1978-06-19T00:00:00","width":900}}]
            """;

    @Test
    void stripImageUrl_takesTheStripForTheRequestedDate() {
        assertThat(GoComicsDownloaderStrategy.stripImageUrl(FLIGHT, LocalDate.of(2026, 10, 8)))
                .contains("https://featureassets.gocomics.com/assets/9ec074b0");
        assertThat(GoComicsDownloaderStrategy.stripImageUrl(FLIGHT, LocalDate.of(1978, 6, 19)))
                .contains("https://featureassets.gocomics.com/assets/239495d0");
    }

    @Test
    void stripImageUrl_emptyWhenNoStripHasTheDate() {
        // 2026-10-07 only appears escaped inside a string, which isn't a strip object
        assertThat(GoComicsDownloaderStrategy.stripImageUrl(FLIGHT, LocalDate.of(2026, 10, 7))).isEmpty();
        assertThat(GoComicsDownloaderStrategy.stripImageUrl("", LocalDate.of(2026, 10, 8))).isEmpty();
    }

    @Test
    void nextJsCacheBuster_matchesTheRouter() {
        // SHA-256 of "0,0,0,/garfield", first 12 bytes, base64url: the value Chrome sent for this navigation
        assertThat(BrowserFetcher.nextJsCacheBuster("/garfield")).isEqualTo("XmGbwMFLp8eH9Ryj");
    }

    @Test
    void chromeClientHints_matchChromeMajorFromUserAgent() {
        String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/154.0.0.0 Safari/537.36";

        assertThat(BrowserFetcher.chromeClientHints(ua))
                .contains("\"Chromium\";v=\"154\", \"Google Chrome\";v=\"154\", \"Not?A_Brand\";v=\"99\"");
    }

    @Test
    void chromeClientHints_emptyForNonChromeUserAgent() {
        String firefox = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:156.0) Gecko/20100101 Firefox/156.0";

        assertThat(BrowserFetcher.chromeClientHints(firefox)).isEmpty();
        assertThat(BrowserFetcher.chromeClientHints(null)).isEmpty();
    }
}
