package org.stapledon.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    @JsonFormat(pattern = "yyyy-MM-dd")
    @ToString.Include
    LocalDate imageDate;
}
