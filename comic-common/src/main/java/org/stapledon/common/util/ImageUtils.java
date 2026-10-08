package org.stapledon.common.util;

import org.stapledon.common.dto.ImageDto;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

public final class ImageUtils {

    private static final DateTimeFormatter IMAGE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private ImageUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * An image's size in pixels.
     */
    public record Dimensions(int width, int height) {
    }

    /**
     * Load an image from the filesystem and return a ImageDto object.
     *
     * @param image Image to Load
     * @return ImageDto object
     */
    public static ImageDto getImageDto(File image) throws IOException {
        byte[] media = Files.readAllBytes(image.toPath());
        Dimensions size;
        try (ImageInputStream in = new MemoryCacheImageInputStream(new ByteArrayInputStream(media))) {
            size = readDimensions(in, image + " (" + media.length + " bytes)");
        }
        return ImageDto.builder()
                .mimeType("image/png")
                .imageData(Base64.getEncoder().withoutPadding().encodeToString(media))
                .height(size.height())
                .width(size.width())
                .imageDate(imageDate(image))
                .build();
    }

    /**
     * Read an image's size from its header, without reading or decoding the rest of the file.
     *
     * @param image Image to read
     * @return its width and height
     * @throws IOException when it can't be read or no ImageIO reader knows its format
     */
    public static Dimensions readDimensions(File image) throws IOException {
        try (ImageInputStream in = new FileImageInputStream(image)) {
            return readDimensions(in, image.toString());
        }
    }

    private static Dimensions readDimensions(ImageInputStream in, String description) throws IOException {
        Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
        if (!readers.hasNext()) {
            throw new IOException("No ImageIO reader could decode " + description);
        }
        ImageReader reader = readers.next();
        try {
            reader.setInput(in, true, true);
            return new Dimensions(reader.getWidth(0), reader.getHeight(0));
        } finally {
            reader.dispose();
        }
    }

    /**
     * The date an image's file name gives ({@code 2026-09-28.png}), or null when the name isn't a date (e.g. avatar.png).
     */
    public static LocalDate imageDate(File image) {
        try {
            return LocalDate.parse(stripExtension(image.getName()), IMAGE_DATE);
        } catch (DateTimeParseException _) {
            return null;
        }
    }

    /**
     * Return a file name without its last extension ({@code 2026-09-28.png} becomes {@code 2026-09-28}).
     * A name with no dot is returned unchanged.
     *
     * @param fileName file name, without a directory
     * @return the name up to its last dot
     */
    public static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? fileName : fileName.substring(0, dot);
    }
}
