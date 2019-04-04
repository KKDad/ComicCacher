package com.stapledon.interop;

import java.time.LocalDate;

public class ComicItem implements Comparable<ComicItem> {
    public ComicItem()
    {
        // No-argument constructor required for gson
    }
    public int id;
    public String name;
    public String author;
    public LocalDate oldest;
    public LocalDate newest;
    public Boolean enabled;

    public String description;

    @Override
    public int compareTo(ComicItem o) {
        return this.name.compareTo(o.name);
    }
}