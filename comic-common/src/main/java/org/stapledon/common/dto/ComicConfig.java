package org.stapledon.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ComicConfig {
    private Map<Integer, ComicItem> items;
    private List<ComicItem> comics;

    public ComicConfig() {
        this.items = new ConcurrentHashMap<>();
        this.comics = new ArrayList<>();
    }

    /**
     * Gets all comics as a list.
     * If the comics list is empty but items map has values, populates the comics list from the items map.
     * This method is marked with @JsonIgnore to prevent serialization of the comics array,
     * ensuring only the items map is persisted (single source of truth).
     *
     * @return List of comic items
     */
    @JsonIgnore
    public List<ComicItem> getComics() {
        if (comics == null) {
            comics = new ArrayList<>();
        }

        // If comics list is empty but we have items, populate comics from items
        if (comics.isEmpty() && items != null && !items.isEmpty()) {
            comics.addAll(items.values());
        }

        return comics;
    }

    /**
     * Sets the comics list and also updates the items map to maintain consistency.
     *
     * @param comics List of comic items
     */
    public void setComics(List<ComicItem> comics) {
        this.comics = comics;

        // Also update items map to maintain consistency
        if (this.items == null) {
            this.items = new ConcurrentHashMap<>();
        }

        // Clear and repopulate items map
        this.items.clear();
        if (comics != null) {
            for (ComicItem comic : comics) {
                this.items.put(comic.getId(), comic);
            }
        }
    }
}