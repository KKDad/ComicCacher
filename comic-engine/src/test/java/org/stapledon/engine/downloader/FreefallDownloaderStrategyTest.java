package org.stapledon.engine.downloader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.stream.Stream;

import org.stapledon.common.infrastructure.web.InspectorService;
import org.stapledon.common.service.ValidationService;

@ExtendWith(MockitoExtension.class)
class FreefallDownloaderStrategyTest {

    @Mock
    private InspectorService webInspector;

    @Mock
    private ValidationService imageValidationService;

    private FreefallDownloaderStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new FreefallDownloaderStrategy(webInspector, imageValidationService);
    }

    @Test
    void shouldHaveCorrectSourceIdentifier() {
        assertThat(strategy.getSource()).isEqualTo("freefall");
    }

    @Test
    void shouldCreateStrategyWithDependencies() {
        InspectorService mockInspector = mock(InspectorService.class);
        ValidationService mockValidation = mock(ValidationService.class);

        FreefallDownloaderStrategy newStrategy = new FreefallDownloaderStrategy(
                mockInspector, mockValidation);

        assertThat(newStrategy).isNotNull();
        assertThat(newStrategy.getSource()).isEqualTo("freefall");
    }

    @ParameterizedTest
    @MethodSource("titlesWithDates")
    void shouldParseTitleWithDate(String title, int expectedStrip, String expectedDate) {
        FreefallDownloaderStrategy.TitleParseResult result = strategy.parseTitleTag(title);

        assertThat(result).isNotNull();
        assertThat(result.stripNumber()).isEqualTo(expectedStrip);
        assertThat(result.date()).isEqualTo(LocalDate.parse(expectedDate));
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> titlesWithDates() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(
                        "Freefall 04350 March 18, 2026", 4350, "2026-03-18"),
                org.junit.jupiter.params.provider.Arguments.of(
                        "Freefall 4351 March 20, 2026", 4351, "2026-03-20"),
                org.junit.jupiter.params.provider.Arguments.of(
                        "Freefall 00100 January 1, 2000", 100, "2000-01-01")
        );
    }

    @Test
    void shouldParseTitleWithoutDate() {
        FreefallDownloaderStrategy.TitleParseResult result = strategy.parseTitleTag("Freefall 00001");

        assertThat(result).isNotNull();
        assertThat(result.stripNumber()).isEqualTo(1);
        assertThat(result.date()).isNull();
    }

    @Test
    void shouldReturnNullForInvalidTitle() {
        assertThat(strategy.parseTitleTag("Not a Freefall title")).isNull();
        assertThat(strategy.parseTitleTag(null)).isNull();
        assertThat(strategy.parseTitleTag("")).isNull();
    }

    @Test
    void shouldParseDateFromHtmlComment() {
        String html = "<html><head><title>Freefall 00001</title></head>"
                + "<body><!- April 9, 1998 -><p>content</p></body></html>";
        Document doc = Jsoup.parse(html);

        LocalDate result = strategy.parseDateFromComment(doc);

        assertThat(result).isEqualTo(LocalDate.of(1998, 4, 9));
    }

    @Test
    void shouldReturnNullWhenNoDateComment() {
        String html = "<html><head><title>Freefall 00001</title></head>"
                + "<body><p>content</p></body></html>";
        Document doc = Jsoup.parse(html);

        LocalDate result = strategy.parseDateFromComment(doc);

        assertThat(result).isNull();
    }

    @Test
    void shouldParseTranscript() {
        String html = "<html><body>"
                + "<div><font size=+1>TRANSCRIPT</font><br>"
                + "Florence: Hello there.<br>"
                + "Sam: Hi Florence!<br>"
                + "</div></body></html>";
        Document doc = Jsoup.parse(html);

        String result = strategy.parseTranscript(doc);

        assertThat(result).isNotNull();
        assertThat(result).contains("Florence");
        assertThat(result).contains("Sam");
    }

    @Test
    void shouldHandleMissingTranscript() {
        String html = "<html><body><p>Just a normal page</p></body></html>";
        Document doc = Jsoup.parse(html);

        String result = strategy.parseTranscript(doc);

        assertThat(result).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "1, 100",
            "100, 100",
            "101, 200",
            "200, 200",
            "201, 300",
            "4350, 4400",
            "4400, 4400",
            "4401, 4500"
    })
    void shouldCalculateFolderNumber(int stripNumber, int expectedFolder) {
        assertThat(strategy.calculateFolderNumber(stripNumber)).isEqualTo(expectedFolder);
    }

    @Test
    void shouldBuildStripPageUrl() {
        String url = strategy.buildStripPageUrl(4350);
        assertThat(url).isEqualTo("http://freefall.purrsia.com/ff4400/fc04350.htm");
    }

    @Test
    void shouldBuildStripPageUrlForLowNumber() {
        String url = strategy.buildStripPageUrl(1);
        assertThat(url).isEqualTo("http://freefall.purrsia.com/ff100/fc00001.htm");
    }

    @Test
    void shouldExtractImageUrl() {
        String html = "<html><body>"
                + "<img src=\"ff4400/fc04350.png\" alt=\"strip\">"
                + "</body></html>";
        Document doc = Jsoup.parse(html);

        String result = strategy.extractImageUrl(doc, 4350);

        assertThat(result).isEqualTo("ff4400/fc04350.png");
    }

    @Test
    void shouldReturnNullWhenNoImageFound() {
        String html = "<html><body><p>No images here</p></body></html>";
        Document doc = Jsoup.parse(html);

        String result = strategy.extractImageUrl(doc, 4350);

        assertThat(result).isNull();
    }
}
