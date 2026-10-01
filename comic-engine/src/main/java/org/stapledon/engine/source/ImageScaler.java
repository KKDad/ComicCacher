package org.stapledon.engine.source;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import javax.imageio.ImageIO;

/**
 * Shrinks catalog thumbnails. Source feature images can be several megabytes; the Sources page shows them at avatar size.
 */
final class ImageScaler {

    private ImageScaler() {
    }

    /**
     * The image scaled down to {@code maxWidth} and encoded as PNG, or empty when it is already narrow enough or can't be decoded (it is then kept
     * as it is).
     */
    static Optional<byte[]> shrinkToWidth(byte[] data, int maxWidth) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
        if (image == null || image.getWidth() <= maxWidth) {
            return Optional.empty();
        }
        int height = Math.max(1, (int) Math.round((double) image.getHeight() * maxWidth / image.getWidth()));
        BufferedImage scaled = new BufferedImage(maxWidth, height, image.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(image, 0, 0, maxWidth, height, null);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(scaled, "png", out);
        return Optional.of(out.toByteArray());
    }
}
