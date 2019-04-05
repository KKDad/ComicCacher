package com.stapledon.dto;

import java.util.ArrayList;
import java.util.List;

public class ComicList
{
    private final List<ComicItem> comics;

    public ComicList()
    {
        comics = new ArrayList<>();
    }

    public List<ComicItem> getComics() {
        return comics;
    }

    public void setComics(List<ComicItem> comics) {
        this.comics.addAll(comics);
    }
}
