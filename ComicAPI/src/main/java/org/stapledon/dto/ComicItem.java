package org.stapledon.dto;

import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Objects;

@SuppressWarnings({"squid:ClassVariableVisibilityCheck"})
@NoArgsConstructor
public class ComicItem implements Comparable<ComicItem> {
    public int id;
    public String name;
    public String author;
    public LocalDate oldest;
    public LocalDate newest;
    public Boolean enabled;
    public String description;

    public Boolean avatarAvailable;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var comicItem = (ComicItem) o;
        return id == comicItem.id &&
               name.equals(comicItem.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public int compareTo(ComicItem other) {
        return this.name.compareTo(other.name);
    }
}