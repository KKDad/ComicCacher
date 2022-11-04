package org.stapledon.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class ComicConfig {
    public ComicConfig() {
        this.items = new ConcurrentHashMap<>();
    }

    Map<Integer, ComicItem> items;
}
