package nl.mijnummah.storage;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.imageio.ImageIO;

/** Images generated in memory for tests. */
public final class TestImages {

    private TestImages() {
    }

    public static byte[] jpeg(int width, int height) {
        return encode(image(width, height, BufferedImage.TYPE_INT_RGB), "jpeg");
    }

    public static byte[] png(int width, int height, boolean transparent) {
        return encode(image(width, height, transparent ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB),
                "png");
    }

    public static byte[] gif(int width, int height) {
        return encode(image(width, height, BufferedImage.TYPE_INT_RGB), "gif");
    }

    private static BufferedImage image(int width, int height, int type) {
        BufferedImage image = new BufferedImage(width, height, type);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(0x12, 0x3C, 0x32));
        graphics.fillRect(0, 0, width / 2, height);
        graphics.dispose();
        return image;
    }

    private static byte[] encode(BufferedImage image, String format) {
        var out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, format, out);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return out.toByteArray();
    }
}
