package org.stapledon.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.stapledon.common.dto.ImageDto;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;


class ImageUtilsTest {

    private static File getResourcesDirectory() {
        File resourcesDirectory = new File("src/test/resources");
        if (!resourcesDirectory.exists()) {
            resourcesDirectory = new File("ComicAPI/src/test/resources");
        }
        assertThat(resourcesDirectory).exists();
        return resourcesDirectory;
    }

    @Test
    void getImageDtoAvatar() throws IOException {
        Path path = Paths.get(getResourcesDirectory().getAbsolutePath(), "FakeComic", "avatar.png");
        ImageDto imageDto = ImageUtils.getImageDto(path.toFile());

        assertThat(imageDto.getMimeType()).isNotNull().isEqualTo("image/png");
        assertThat(imageDto.getHeight()).isEqualTo(1);
        assertThat(imageDto.getWidth()).isEqualTo(1);
        assertThat(imageDto.getImageDate()).isNull();
    }

    @Test
    void getImageDtoStrip() throws IOException {
        Path path = Paths.get(getResourcesDirectory().getAbsolutePath(), "FakeComic", "2008", "2008-01-11.png");
        ImageDto imageDto = ImageUtils.getImageDto(path.toFile());

        assertThat(imageDto.getMimeType()).isNotNull().isEqualTo("image/png");
        assertThat(imageDto.getHeight()).isEqualTo(1);
        assertThat(imageDto.getWidth()).isEqualTo(1);
        assertThat(imageDto.getImageDate()).isEqualTo(LocalDate.of(2008, 1, 11));

    }
}