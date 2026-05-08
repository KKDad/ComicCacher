package org.stapledon.common.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class ImageDto {
    @ToString.Include
    String mimeType;

    String imageData;

    @ToString.Include
    Integer height;

    @ToString.Include
    Integer width;

    @ToString.Include
    LocalDate imageDate;

    String transcript;
}
