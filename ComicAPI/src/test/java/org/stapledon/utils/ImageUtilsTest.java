package org.stapledon.utils;

import org.junit.jupiter.api.Test;
import org.stapledon.CacheUtilsTest;
import org.stapledon.dto.ImageDto;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;


class ImageUtilsTest {

    @Test
    void getImageDtoAvatar() throws IOException {
        Path path = Paths.get(CacheUtilsTest.getResourcesDirectory().getAbsolutePath(), "FakeComic", "avatar.png");
        ImageDto imageDto = ImageUtils.getImageDto(path.toFile());

        assertThat(imageDto.mimeType).isEqualTo("image/png");
        assertThat(imageDto.height).isEqualTo(1);
        assertThat(imageDto.width).isEqualTo(1);
        assertThat(imageDto.imageDate).isNull();
    }

    @Test
    void getImageDtoStrip() throws IOException {
        Path path = Paths.get(CacheUtilsTest.getResourcesDirectory().getAbsolutePath(), "FakeComic", "2008", "2008-01-11.png");
        ImageDto imageDto = ImageUtils.getImageDto(path.toFile());

        assertThat(imageDto.mimeType).isEqualTo("image/png");
        assertThat(imageDto.height).isEqualTo(1);
        assertThat(imageDto.width).isEqualTo(1);
        assertThat(imageDto.imageDate).isEqualTo(LocalDate.of(2008, 01, 11));

    }
}