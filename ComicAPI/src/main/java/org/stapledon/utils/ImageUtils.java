package org.stapledon.utils;

import org.springframework.http.MediaType;
import org.stapledon.dto.ImageDto;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

public class ImageUtils {

    private ImageUtils() {
        // Sonar: No public constructor
    }


    /**
     * Load an image from the filesystem and return a ImageDto object
     * @param image Image to Load
     * @return ImageDto object
     */
    public static ImageDto getImageDto(File image) throws IOException
    {
        byte[] media = Files.readAllBytes(image.toPath());

        BufferedImage bi = ImageIO.read(image);

        ImageDto dto = new ImageDto();
        dto.mimeType = MediaType.IMAGE_PNG.toString();
        dto.imageData = Base64.getEncoder().withoutPadding().encodeToString(media);
        dto.height = bi.getHeight();
        dto.width = bi.getWidth();
        return dto;
    }
}
