package com.stapledon.interop;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ComicConfig {
    public ComicConfig()
    {
        this.items = new ConcurrentHashMap<>();
    }

    public Map<Integer,ComicItem> items;
}
