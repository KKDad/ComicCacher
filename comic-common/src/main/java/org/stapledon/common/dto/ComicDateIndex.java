package org.stapledon.common.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Persistent index of available comic dates for a specific comic.
 * Saved as available-dates.json in the comic's directory.
 *
 * Note: Serialized using Gson with custom LocalDate adapter (GsonUtils).
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class ComicDateIndex {

    @ToString.Include
    private int comicId;

    @ToString.Include
    private String comicName;

    /**
     * Sorted list of all dates that have a comic strip available on disk.
     * Dates are stored in ISO-8601 format (yyyy-MM-dd) when serialized.
     */
    private List<LocalDate> availableDates;

    /**
     * Date when the index was last updated.
     */
    private LocalDate lastUpdated;
}
