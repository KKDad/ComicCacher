package org.stapledon.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
@Builder
public class ImageCacheStats {
    private String oldestImage;
    private String newestImage;
    private List<String> years;
}
