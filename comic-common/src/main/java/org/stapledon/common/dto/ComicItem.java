package org.stapledon.common.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@AllArgsConstructor
@Builder
@Getter
@Setter
@NoArgsConstructor
public class ComicItem implements Comparable<ComicItem> {
    int id;
    String name;
    String author;
    LocalDate oldest;
    LocalDate newest;
    Boolean enabled;
    String description;
    Boolean avatarAvailable;
    String source;
    String sourceIdentifier;
    List<DayOfWeek> publicationDays; // Days comic publishes (null/empty = daily)
    Boolean active; // Whether comic is actively publishing (true = active, false = inactive/discontinued)

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var comicItem = (ComicItem) o;
        return getId() == comicItem.getId() &&
                getName().equals(comicItem.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName());
    }

    @Override
    public int compareTo(ComicItem other) {
        if (this.name == null && other.name == null) return 0;
        if (this.name == null) return -1;
        if (other.name == null) return 1;
        return this.name.compareTo(other.name);
    }

    /**
     * Checks if this comic has an avatar available.
     *
     * @return true if avatar is available, false otherwise
     */
    public boolean isAvatarAvailable() {
        return avatarAvailable != null && avatarAvailable;
    }

    /**
     * Checks if this comic is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled != null && enabled;
    }

    /**
     * Checks if this comic is actively publishing new strips.
     *
     * @return true if active, false if inactive/discontinued
     */
    public boolean isActive() {
        return active == null || active;
    }
}
