package com.stapledon.utils;

import com.stapledon.dto.ComicItem;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

public class CacheUtils
{
    private final String cacheHome;

    public CacheUtils(String cacheHome)
    {
        Objects.requireNonNull(cacheHome, "cacheHome must be specified");

        this.cacheHome = cacheHome;
    }

    public File findFirst(ComicItem comic, Direction which)
    {
        String comicNameParsed = comic.name.replace(" ", "");
        String path = String.format("%s/%s", this.cacheHome, comicNameParsed);
        File rootfolder = new File(path);

        // Comics are stored by year, find the smallest year
        String[] files = rootfolder.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return !name.toLowerCase().contains("avatar");
            }
        });
        if (files == null || files.length == 0)
            return null;
        Arrays.sort(files, Comparator.comparing(Integer::valueOf));

        // Comics are stored with filename that is sortable.
        File folder = new File(String.format("%s/%s", rootfolder.getAbsolutePath(), which == Direction.FORWARD ? files[0] : files[files.length - 1]));
        files = folder.list();
        if (files == null || files.length == 0)
            return null;
        Arrays.sort(files, String::compareTo);

        return new File(String.format("%s/%s", folder.getAbsolutePath(), which == Direction.FORWARD ? files[0] : files[files.length - 1]));
    }
}
