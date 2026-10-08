package org.stapledon.engine.downloader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.jsoup.HttpStatusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.stapledon.common.dto.ComicDownloadResult;
import org.stapledon.common.dto.ComicDownloadResult.FailureKind;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ImageFormat;
import org.stapledon.common.dto.ImageValidationResult;
import org.stapledon.common.infrastructure.web.InspectorService;
import org.stapledon.common.infrastructure.web.UserAgentService;
import org.stapledon.common.service.ValidationService;

@ExtendWith(MockitoExtension.class)
class XkcdDownloaderStrategyTest {

    private static final String LATEST_JSON = """
            {"month": "10", "num": 3308, "link": "", "year": "2026", "news": "", "safe_title": "Juice", "transcript": "",
             "alt": "I need to push some updates to the remote sensing instruments.", "img": "https://imgs.xkcd.com/comics/juice.png",
             "title": "Juice", "day": "7"}
            """;
    private static final String FIRST_JSON = """
            {"month": "1", "num": 1, "link": "", "year": "2006", "news": "", "safe_title": "Barrel - Part 1", "transcript": "",
             "alt": "Don't we all.", "img": "https://imgs.xkcd.com/comics/barrel_cropped_(1).jpg", "title": "Barrel - Part 1", "day": "1"}
            """;
    // Interactive strip: the API gives the bare image directory
    private static final String HOVERBOARD_JSON = """
            {"month": "11", "num": 1608, "link": "", "year": "2015", "news": "", "safe_title": "Hoverboard", "transcript": "",
             "alt": "I'm sure I'll be fine.", "img": "https://imgs.xkcd.com/comics/", "title": "Hoverboard", "day": "24"}
            """;
    private static final byte[] IMAGE = {1, 2, 3};

    @Mock
    private InspectorService webInspector;

    @Mock
    private ValidationService imageValidationService;

    @Mock
    private UserAgentService userAgentService;

    @Mock
    private SourceThrottleService throttleService;

    private StubbedXkcd strategy;
    private final ComicItem comic = ComicItem.builder().id(42).name("xkcd").source("xkcd").sourceIdentifier("xkcd").build();

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-08T12:00:00Z"), ZoneId.of("America/Toronto"));
        strategy = new StubbedXkcd(webInspector, imageValidationService, userAgentService, throttleService, clock);
    }

    @Test
    void shouldHaveCorrectSourceIdentifier() {
        assertThat(strategy.getSource()).isEqualTo("xkcd");
    }

    @Test
    void parsesTheStripNumberDateImageAndAltText() {
        XkcdDownloaderStrategy.XkcdStrip strip = strategy.parseStrip(LATEST_JSON);

        assertThat(strip.num()).isEqualTo(3308);
        assertThat(strip.date()).isEqualTo(LocalDate.of(2026, 10, 7));
        assertThat(strip.img()).isEqualTo("https://imgs.xkcd.com/comics/juice.png");
        assertThat(strip.safeTitle()).isEqualTo("Juice");
        assertThat(strip.alt()).isEqualTo("I need to push some updates to the remote sensing instruments.");
        assertThat(strip.hasImage()).isTrue();
    }

    @Test
    void treatsBlankAltTextAsNone() {
        XkcdDownloaderStrategy.XkcdStrip strip = strategy.parseStrip(LATEST_JSON.replace(
                "\"I need to push some updates to the remote sensing instruments.\"", "\" \""));

        assertThat(strip.alt()).isNull();
    }

    @Test
    void anInteractiveStripHasNoImage() {
        assertThat(strategy.parseStrip(HOVERBOARD_JSON).hasImage()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "not json",
        "{}",
        "{\"num\": 5, \"year\": \"2006\", \"month\": \"13\", \"day\": \"1\"}",
        "{\"num\": 5, \"img\": \"https://imgs.xkcd.com/comics/x.png\"}"
    })
    void rejectsJsonWithoutAStripNumberOrDate(String json) {
        assertThatThrownBy(() -> strategy.parseStrip(json)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void downloadsTheLatestStripFromTheApi() {
        strategy.json.put(XkcdDownloaderStrategy.BASE_URL + "/info.0.json", LATEST_JSON);
        validImage();

        ComicDownloadResult result = strategy.downloadLatestStrip(comic);

        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getStripNumber()).isEqualTo(3308);
        assertThat(result.getActualDate()).isEqualTo(LocalDate.of(2026, 10, 7));
        assertThat(result.getTranscript()).isEqualTo("I need to push some updates to the remote sensing instruments.");
        assertThat(result.getImageData()).isEqualTo(IMAGE);
        assertThat(strategy.imageUrls).containsExactly("https://imgs.xkcd.com/comics/juice.png");
    }

    @Test
    void downloadsAStripByNumber() {
        strategy.json.put(XkcdDownloaderStrategy.BASE_URL + "/1/info.0.json", FIRST_JSON);
        validImage();

        ComicDownloadResult result = strategy.downloadStrip(comic, 1);

        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getStripNumber()).isEqualTo(1);
        assertThat(result.getActualDate()).isEqualTo(LocalDate.of(2006, 1, 1));
    }

    @Test
    void reportsAnInteractiveStripAsUnavailableWithoutFetchingAnImage() {
        strategy.json.put(XkcdDownloaderStrategy.BASE_URL + "/1608/info.0.json", HOVERBOARD_JSON);

        ComicDownloadResult result = strategy.downloadStrip(comic, 1608);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getFailureKind()).isEqualTo(FailureKind.UNAVAILABLE);
        assertThat(result.getErrorMessage()).contains("#1608", "Hoverboard");
        assertThat(strategy.imageUrls).isEmpty();
    }

    @Test
    void reportsStrip404AsUnavailable() {
        // No JSON registered: the stub answers 404, as xkcd does for /404/info.0.json
        ComicDownloadResult result = strategy.downloadStrip(comic, 404);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getFailureKind()).isEqualTo(FailureKind.UNAVAILABLE);
        assertThat(result.getHttpStatus()).isEqualTo(404);
    }

    private void validImage() {
        when(imageValidationService.validate(any())).thenReturn(ImageValidationResult.success(ImageFormat.PNG, 740, 300, IMAGE.length));
    }

    /** Serves JSON from a map (404 for anything else) and records image URLs instead of making requests. */
    private static final class StubbedXkcd extends XkcdDownloaderStrategy {
        private final Map<String, String> json = new HashMap<>();
        private final List<String> imageUrls = new ArrayList<>();

        StubbedXkcd(InspectorService webInspector, ValidationService imageValidationService, UserAgentService userAgentService,
                SourceThrottleService throttleService, Clock clock) {
            super(webInspector, imageValidationService, userAgentService, throttleService, clock);
        }

        @Override
        String fetchJson(String url) throws IOException {
            String body = json.get(url);
            if (body == null) {
                throw new HttpStatusException("HTTP error fetching URL", 404, url);
            }
            return body;
        }

        @Override
        protected byte[] downloadImageData(String imageUrl) {
            imageUrls.add(imageUrl);
            return IMAGE;
        }
    }
}
