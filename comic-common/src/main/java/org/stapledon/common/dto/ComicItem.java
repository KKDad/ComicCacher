package org.stapledon.common.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ComicItem implements Comparable<ComicItem> {
    @ToString.Include
    int id;

    @ToString.Include
    String name;

    String author;
    LocalDate oldest;
    LocalDate newest;

    @Builder.Default
    boolean enabled = true;

    String description;

    @Builder.Default
    boolean avatarAvailable = false;

    String source;
    String sourceIdentifier;
    List<DayOfWeek> publicationDays; // Days comic publishes (null/empty = daily)

    @Builder.Default
    boolean active = true; // Whether comic is actively publishing (true = active, false = inactive/discontinued)

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var comicItem = (ComicItem) o;
        return getId() == comicItem.getId()
                && getName().equals(comicItem.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName());
    }

    @Override
    public int compareTo(ComicItem other) {
        return Comparator.comparing(ComicItem::getName,
                                   Comparator.nullsFirst(String::compareTo))
                        .compare(this, other);
    }
}
