package org.stapledon.common.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
public class ComicConfig {
    @ToString.Include
    private Map<Integer, ComicItem> items;

    // Transient so Gson skips this field during serialization. The items map is the single source
    // of truth; comics is a derived view exposed by the getter for callers.
    private transient List<ComicItem> comics;

    public ComicConfig() {
        this.items = new ConcurrentHashMap<>();
        this.comics = new ArrayList<>();
    }

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
