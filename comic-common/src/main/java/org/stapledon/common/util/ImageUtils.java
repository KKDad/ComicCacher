package org.stapledon.common.util;

import org.stapledon.common.dto.ImageDto;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;

import javax.imageio.ImageIO;

public class ImageUtils {

    private ImageUtils() {
        // Sonar: No public constructor
    }


    /**
     * Load an image from the filesystem and return a ImageDto object
     *
     * @param image Image to Load
     * @return ImageDto object
     */
    public static ImageDto getImageDto(File image) throws IOException {
        byte[] media = Files.readAllBytes(image.toPath());
        ImageDto imageDto = null;
        try (InputStream is = new ByteArrayInputStream(media)) {
            BufferedImage bi = ImageIO.read(is);
            imageDto = ImageDto.builder()
                    .mimeType("image/png")
                    .imageData(Base64.getEncoder().withoutPadding().encodeToString(media))
                    .height(bi.getHeight())
                    .width(bi.getWidth())
                    .build();
            imageDto.setImageDate(LocalDate.parse(com.google.common.io.Files.getNameWithoutExtension(image.getName()), DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        } catch (DateTimeParseException dte) {
            // Ignore if we don't have a date
        }
        return imageDto;
    }
}
