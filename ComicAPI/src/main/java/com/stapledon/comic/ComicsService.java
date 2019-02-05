package com.stapledon.comic;

import com.google.gson.Gson;
import com.stapledon.interop.ComicConfig;
import com.stapledon.interop.ComicItem;
import com.stapledon.interop.ComicList;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class ComicsService
{
    private static final Logger logger = Logger.getLogger(ComicsService.class.getName());

    protected static List<ComicItem> comics = new ArrayList<>();

    public ComicItem retrieveComic(String id)
    {
        int i = Integer.parseInt(id);
        return comics.stream().filter(p -> p.id == i).findFirst().orElse(null);
    }

    public ComicList retrieveAll()
    {
        ComicList list = new ComicList();
        list.getComics().addAll(comics);
        Collections.sort(list.getComics());
        return list;
    }
}
