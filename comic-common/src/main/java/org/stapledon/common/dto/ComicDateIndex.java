package org.stapledon.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Persistent index of available comic dates for a specific comic.
 * Saved as available-dates.json in the comic's directory.
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
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private List<LocalDate> availableDates;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastUpdated;
}
