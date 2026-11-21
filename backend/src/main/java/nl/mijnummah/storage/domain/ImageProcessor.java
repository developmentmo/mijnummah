package nl.mijnummah.storage.domain;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import nl.mijnummah.storage.InvalidImageException;

/**
 * Checks and re-encodes an uploaded image. Only JPEG and PNG are accepted. The image is decoded and written again,
 * so nothing from the original file survives except the pixels: metadata such as GPS coordinates and anything
 * hidden in the file are gone. Large images are scaled down.
 */
public final class ImageProcessor {

    /** Refused before decoding, so a small file that claims huge dimensions cannot exhaust memory. */
    static final long MAX_PIXELS = 40_000_000L;
    static final int MIN_SIDE = 32;
    static final int MAX_STORED_SIDE = 2400;
    private static final float JPEG_QUALITY = 0.85f;

    private ImageProcessor() {
    }

    public static EncodedImage process(byte[] content) {
        BufferedImage source = decode(content);
        BufferedImage scaled = scaleDown(source);
        boolean transparent = scaled.getColorModel().hasAlpha();
        return transparent ? png(scaled) : jpeg(scaled);
    }

    private static BufferedImage decode(byte[] content) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            Iterator<ImageReader> readers = input == null ? null : ImageIO.getImageReaders(input);
            if (readers == null || !readers.hasNext()) {
                throw new InvalidImageException("The file is not a JPEG or PNG image.");
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!format.equals("jpeg") && !format.equals("png")) {
                    throw new InvalidImageException("Only JPEG and PNG images are accepted.");
                }
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if ((long) width * height > MAX_PIXELS) {
                    throw new InvalidImageException("The image has too many pixels.");
                }
                if (width < MIN_SIDE || height < MIN_SIDE) {
                    throw new InvalidImageException("The image must be at least " + MIN_SIDE + " by " + MIN_SIDE
                            + " pixels.");
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException ex) {
            if (ex instanceof InvalidImageException invalid) {
                throw invalid;
            }
            // corrupt files, CMYK JPEGs and other variants ImageIO cannot read
            throw new InvalidImageException("The image could not be read. Save it as JPEG or PNG and try again.");
        }
    }

    private static BufferedImage scaleDown(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        double factor = Math.min(1.0, (double) MAX_STORED_SIDE / Math.max(width, height));
        int targetWidth = Math.max(1, (int) Math.round(width * factor));
        int targetHeight = Math.max(1, (int) Math.round(height * factor));
        boolean alpha = source.getColorModel().hasAlpha();
        BufferedImage target = new BufferedImage(targetWidth, targetHeight,
                alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private static EncodedImage png(BufferedImage image) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", out);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return new EncodedImage(out.toByteArray(), "image/png", image.getWidth(), image.getHeight());
    }

    private static EncodedImage jpeg(BufferedImage image) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(output);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(image, null, null), param);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        } finally {
            writer.dispose();
        }
        return new EncodedImage(out.toByteArray(), "image/jpeg", image.getWidth(), image.getHeight());
    }
}
