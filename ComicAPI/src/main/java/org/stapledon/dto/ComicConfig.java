package org.stapledon.dto;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ComicConfig {
    public ComicConfig() {
        this.items = new ConcurrentHashMap<>();
    }

    public Map<Integer, ComicItem> items;
}
