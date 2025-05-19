package org.stapledon.api.dto.comic;

import java.time.LocalDate;
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
        return this.getName().compareTo(other.getName());
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
}