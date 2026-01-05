package org.stapledon.common.dto;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

/**
 * Data Transfer Object representing the result of a comic download operation.
 * Contains the downloaded image data and status information about the download.
 */
@Data
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class ComicDownloadResult {
    /**
     * The request that initiated this download.
     */
    @ToString.Include
    private final ComicDownloadRequest request;

    /**
     * The binary image data of the downloaded comic.
     */
    private final byte[] imageData;

    /**
     * Flag indicating whether the download was successful.
     */
    @ToString.Include
    private final boolean successful;

    /**
     * Error message in case the download failed.
     */
    private final String errorMessage;

    /**
     * Factory method to create a successful result.
     *
     * @param request   The original download request
     * @param imageData The downloaded image data
     * @return A successful ComicDownloadResult
     */
    public static ComicDownloadResult success(ComicDownloadRequest request, byte[] imageData) {
        return ComicDownloadResult.builder()
                .request(request)
                .imageData(imageData)
                .successful(true)
                .build();
    }

    /**
     * Factory method to create a failed result.
     *
     * @param request      The original download request
     * @param errorMessage Description of the error
     * @return A failed ComicDownloadResult
     */
    public static ComicDownloadResult failure(ComicDownloadRequest request, String errorMessage) {
        return ComicDownloadResult.builder()
                .request(request)
                .successful(false)
                .errorMessage(errorMessage)
                .build();
    }
}