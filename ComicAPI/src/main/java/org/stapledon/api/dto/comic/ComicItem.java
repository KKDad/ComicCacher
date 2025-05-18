package org.stapledon.api.dto.comic;

import lombok.*;

import java.time.LocalDate;
import java.util.Objects;


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
}