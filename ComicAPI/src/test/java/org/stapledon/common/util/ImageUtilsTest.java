package org.stapledon.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.stapledon.api.dto.comic.ImageDto;
import org.stapledon.infrastructure.caching.CacheUtilsTest;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;


class ImageUtilsTest {

    @Test
    void getImageDtoAvatar() throws IOException {
        Path path = Paths.get(CacheUtilsTest.getResourcesDirectory().getAbsolutePath(), "FakeComic", "avatar.png");
        ImageDto imageDto = ImageUtils.getImageDto(path.toFile());

        assertThat(imageDto.getMimeType()).isNotNull().isEqualTo("image/png");
        assertThat(imageDto.getHeight()).isEqualTo(1);
        assertThat(imageDto.getWidth()).isEqualTo(1);
        assertThat(imageDto.getImageDate()).isNull();
    }

    @Test
    void getImageDtoStrip() throws IOException {
        Path path = Paths.get(CacheUtilsTest.getResourcesDirectory().getAbsolutePath(), "FakeComic", "2008", "2008-01-11.png");
        ImageDto imageDto = ImageUtils.getImageDto(path.toFile());

        assertThat(imageDto.getMimeType()).isNotNull().isEqualTo("image/png");
        assertThat(imageDto.getHeight()).isEqualTo(1);
        assertThat(imageDto.getWidth()).isEqualTo(1);
        assertThat(imageDto.getImageDate()).isEqualTo(LocalDate.of(2008, 1, 11));

    }
}