package org.stapledon.common.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class ComicList {
    @ToString.Include
    @Builder.Default
    private List<ComicItem> comics = new ArrayList<>();

    // Custom setter preserving addAll behavior for backward compatibility
    public void setComics(List<ComicItem> comics) {
        this.comics.clear();
        this.comics.addAll(comics);
    }
}
