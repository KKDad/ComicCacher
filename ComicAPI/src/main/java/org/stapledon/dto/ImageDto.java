package org.stapledon.dto;

import lombok.*;

import java.time.LocalDate;

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
