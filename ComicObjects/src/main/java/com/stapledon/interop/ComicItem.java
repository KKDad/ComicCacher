package com.stapledon.interop;

import java.time.LocalDate;
import java.util.UUID;

public class ComicItem {
    public ComicItem()
    {
        // No-argument constructor required for gson
    }
    public UUID id;
    public String name;
    public String description;
    public LocalDate oldest;
    public LocalDate newest;
}
