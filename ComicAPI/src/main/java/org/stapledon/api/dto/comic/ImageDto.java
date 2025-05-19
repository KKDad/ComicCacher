package org.stapledon.api.dto.comic;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImageDto {
    String mimeType;
    String imageData;
    Integer height;
    Integer width;
    LocalDate imageDate;
}
