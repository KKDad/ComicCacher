package org.stapledon.utils;

import org.springframework.http.MediaType;
import org.stapledon.dto.ImageDto;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;

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
        BufferedImage bi = ImageIO.read(image);
        ImageDto dto = new ImageDto();
        try {
            dto.mimeType = MediaType.IMAGE_PNG.toString();
            dto.imageData = Base64.getEncoder().withoutPadding().encodeToString(media);
            dto.height = bi.getHeight();
            dto.width = bi.getWidth();
            dto.imageDate = LocalDate.parse(com.google.common.io.Files.getNameWithoutExtension(image.getName()), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (DateTimeParseException dte) {
            // Ignore if we don't have a date
        }
        return dto;
    }
}
