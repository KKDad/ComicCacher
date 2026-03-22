package org.stapledon.engine.downloader;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;

import org.stapledon.common.dto.ComicDownloadRequest;
import org.stapledon.common.dto.ComicDownloadResult;
import org.stapledon.common.dto.ComicItem;
import org.stapledon.common.dto.ImageValidationResult;
import org.stapledon.common.infrastructure.web.InspectorService;
import org.stapledon.common.service.ValidationService;

/**
 * Abstract base class for strip-number-based (indexed) comic downloader strategies.
 * Provides the template methods for downloadLatestStrip and downloadStrip that handle
 * validation and error handling.
 */
@Slf4j
@ToString(callSuper = true)
public abstract class AbstractIndexedDownloaderStrategy extends AbstractComicDownloaderStrategy
        implements IndexedComicDownloaderStrategy {

    /**
     * Creates a new indexed downloader strategy for the specified source.
     *
     * @param source The source identifier (e.g., "freefall")
     * @param webInspector The web inspector to use for HTTP requests
     * @param imageValidationService The service for validating downloaded images
     */
    protected AbstractIndexedDownloaderStrategy(String source,
            InspectorService webInspector,
            ValidationService imageValidationService) {
        super(source, webInspector, imageValidationService);
    }

    /**
     * Data returned by concrete strategies after fetching an indexed strip.
     * Contains the raw image data and metadata discovered from the page.
     */
    protected record IndexedStripData(byte[] imageData, LocalDate actualDate,
            int stripNumber, String transcript) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComicDownloadResult downloadLatestStrip(ComicItem comic) {
        try {
            IndexedStripData data = fetchLatestStrip(comic);
            return buildSuccessResult(comic, data);
        } catch (Exception e) {
            String errorMessage = String.format("Error downloading latest strip for %s: %s",
                    comic.getName(), e.getMessage());
            log.error(errorMessage, e);
            return ComicDownloadResult.failure(buildRequest(comic, LocalDate.now()), errorMessage);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComicDownloadResult downloadStrip(ComicItem comic, int stripNumber) {
        try {
            IndexedStripData data = fetchStrip(comic, stripNumber);
            return buildSuccessResult(comic, data);
        } catch (Exception e) {
            String errorMessage = String.format("Error downloading strip #%d for %s: %s",
                    stripNumber, comic.getName(), e.getMessage());
            log.error(errorMessage, e);
            return ComicDownloadResult.failure(buildRequest(comic, LocalDate.now()), errorMessage);
        }
    }

    private ComicDownloadResult buildSuccessResult(ComicItem comic, IndexedStripData data) {
        // Validate image integrity using shared helper
        ImageValidationResult validation = validateImage(data.imageData(),
                comic.getName(), "strip #" + data.stripNumber());
        if (validation == null || !validation.isValid()) {
            String detail = validation != null ? validation.getErrorMessage() : "empty data";
            ComicDownloadRequest request = buildRequest(comic, data.actualDate());
            return ComicDownloadResult.failure(request,
                    String.format("Invalid image for strip #%d: %s", data.stripNumber(), detail));
        }

        ComicDownloadRequest request = buildRequest(comic, data.actualDate());
        return ComicDownloadResult.successWithMetadata(request, data.imageData(),
                data.actualDate(), data.stripNumber(), data.transcript());
    }

    /**
     * Builds a download request from a comic and date.
     */
    protected ComicDownloadRequest buildRequest(ComicItem comic, LocalDate date) {
        return ComicDownloadRequest.builder()
                .comicId(comic.getId())
                .comicName(comic.getName())
                .source(getSource())
                .sourceIdentifier(comic.getSourceIdentifier())
                .date(date != null ? date : LocalDate.now())
                .build();
    }

    /**
     * Fetches the latest strip from the source.
     * Concrete strategies implement this to handle source-specific page fetching and parsing.
     *
     * @param comic The comic to fetch the latest strip for
     * @return The fetched strip data including image, date, strip number, and transcript
     * @throws Exception If an error occurs during fetching
     */
    protected abstract IndexedStripData fetchLatestStrip(ComicItem comic) throws Exception;

    /**
     * Fetches a specific strip by number from the source.
     * Concrete strategies implement this to handle source-specific page fetching and parsing.
     *
     * @param comic The comic to fetch a strip for
     * @param stripNumber The strip number to fetch
     * @return The fetched strip data including image, date, strip number, and transcript
     * @throws Exception If an error occurs during fetching
     */
    protected abstract IndexedStripData fetchStrip(ComicItem comic, int stripNumber) throws Exception;
}
