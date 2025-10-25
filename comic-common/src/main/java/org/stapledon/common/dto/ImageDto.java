package org.stapledon.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

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

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate imageDate;
}
