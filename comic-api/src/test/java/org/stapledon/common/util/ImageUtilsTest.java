package org.stapledon.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.stapledon.common.dto.ImageDto;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

class ImageUtilsTest {

    private static File getResourcesDirectory() {
        File resourcesDirectory = new File("src/test/resources");
        if (!resourcesDirectory.exists()) {
            resourcesDirectory = new File("comic-api/src/test/resources");
        }
        assertThat(resourcesDirectory).exists();
        return resourcesDirectory;
    }

    @Test
    void getImageDtoAvatar() throws Exception {
        Path path = Path.of(getResourcesDirectory().getAbsolutePath(), "FakeComic", "avatar.png");
        ImageDto imageDto = ImageUtils.getImageDto(path.toFile());

        assertThat(imageDto.getMimeType()).isNotNull().isEqualTo("image/png");
        assertThat(imageDto.getHeight()).isEqualTo(1);
        assertThat(imageDto.getWidth()).isEqualTo(1);
        assertThat(imageDto.getImageDate()).isNull();
    }

    @Test
    void getImageDtoStrip() throws Exception {
        Path path = Path.of(getResourcesDirectory().getAbsolutePath(), "FakeComic", "2008", "2008-01-11.png");
        ImageDto imageDto = ImageUtils.getImageDto(path.toFile());

        assertThat(imageDto.getMimeType()).isNotNull().isEqualTo("image/png");
        assertThat(imageDto.getHeight()).isEqualTo(1);
        assertThat(imageDto.getWidth()).isEqualTo(1);
        assertThat(imageDto.getImageDate()).isEqualTo(LocalDate.of(2008, 1, 11));

    }

    @Test
    void getImageDtoRejectsUndecodableFileWithItsPath(@TempDir Path tempDir) throws Exception {
        Path notAnImage = tempDir.resolve("2026-09-24.png");
        Files.writeString(notAnImage, "this is not a png");

        // ImageIO.read returns null for unknown formats; this used to surface as a NullPointerException
        assertThatThrownBy(() -> ImageUtils.getImageDto(notAnImage.toFile()))
                .isInstanceOf(IOException.class)
                .hasMessageContaining(notAnImage.toString());
    }

    @Test
    void readDimensionsReadsOnlyTheHeader(@TempDir Path tempDir) throws Exception {
        Path png = tempDir.resolve("2026-09-24.png");
        ImageIO.write(new BufferedImage(640, 200, BufferedImage.TYPE_INT_RGB), "png", png.toFile());

        assertThat(ImageUtils.readDimensions(png.toFile())).isEqualTo(new ImageUtils.Dimensions(640, 200));
    }

    @Test
    void readDimensionsRejectsUndecodableFileWithItsPath(@TempDir Path tempDir) throws Exception {
        Path notAnImage = tempDir.resolve("2026-09-24.png");
        Files.writeString(notAnImage, "this is not a png");

        assertThatThrownBy(() -> ImageUtils.readDimensions(notAnImage.toFile()))
                .isInstanceOf(IOException.class)
                .hasMessageContaining(notAnImage.toString());
    }
}
