package org.stapledon.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Result of a comic strip navigation request.
 * Contains the image if found, or helpful metadata about why it wasn't found
 * and what dates are available.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ComicNavigationResult {

    /**
     * Whether the requested image was found
     */
    private boolean found;

    /**
     * The image data (null if not found)
     */
    private ImageDto image;

    /**
     * Human-readable reason when image not found
     * Possible values: "AT_END", "AT_BEGINNING", "NO_COMICS_AVAILABLE"
     */
    private String reason;

    /**
     * The date that was requested
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate requestedDate;

    /**
     * Nearest available date going backward from the requested date (null if none)
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate nearestPreviousDate;

    /**
     * Nearest available date going forward from the requested date (null if none)
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate nearestNextDate;

    /**
     * The date of the current image being displayed (same as image.imageDate when found)
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate currentDate;

    /**
     * Creates a successful result with an image
     */
    public static ComicNavigationResult found(ImageDto image, LocalDate nearestPrev, LocalDate nearestNext) {
        return ComicNavigationResult.builder()
                .found(true)
                .image(image)
                .currentDate(image.getImageDate())
                .nearestPreviousDate(nearestPrev)
                .nearestNextDate(nearestNext)
                .build();
    }

    /**
     * Creates a not-found result with boundary information
     */
    public static ComicNavigationResult notFound(String reason, LocalDate requestedDate,
                                                   LocalDate nearestPrev, LocalDate nearestNext) {
        return ComicNavigationResult.builder()
                .found(false)
                .reason(reason)
                .requestedDate(requestedDate)
                .nearestPreviousDate(nearestPrev)
                .nearestNextDate(nearestNext)
                .build();
    }
}
