package nl.mijnummah.storage.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import nl.mijnummah.storage.InvalidImageException;
import nl.mijnummah.storage.TestImages;

class ImageProcessorTest {

    @Test
    void keepsTheSizeOfAnOrdinaryPhotoAndReencodesItAsJpeg() {
        EncodedImage image = ImageProcessor.process(TestImages.jpeg(800, 600));
        assertThat(image.contentType()).isEqualTo("image/jpeg");
        assertThat(image.width()).isEqualTo(800);
        assertThat(image.height()).isEqualTo(600);
    }

    @Test
    void scalesALargeImageDownKeepingItsShape() {
        EncodedImage image = ImageProcessor.process(TestImages.jpeg(4800, 2400));
        assertThat(image.width()).isEqualTo(ImageProcessor.MAX_STORED_SIDE);
        assertThat(image.height()).isEqualTo(ImageProcessor.MAX_STORED_SIDE / 2);
    }

    @Test
    void keepsTransparencyAsPngAndStoresOpaquePngsAsJpeg() {
        assertThat(ImageProcessor.process(TestImages.png(200, 200, true)).contentType()).isEqualTo("image/png");
        assertThat(ImageProcessor.process(TestImages.png(200, 200, false)).contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void dropsEverythingButThePixels() {
        byte[] marker = "GPS 52.0907 5.1214 hidden".getBytes(StandardCharsets.US_ASCII);
        var withComment = new ByteArrayOutputStream();
        byte[] jpeg = TestImages.jpeg(100, 100);
        // a JPEG comment segment (FF FE) right after the start-of-image marker, as cameras write metadata
        withComment.write(jpeg, 0, 2);
        withComment.write(0xFF);
        withComment.write(0xFE);
        withComment.write(0);
        withComment.write(marker.length + 2);
        withComment.writeBytes(marker);
        withComment.write(jpeg, 2, jpeg.length - 2);

        EncodedImage image = ImageProcessor.process(withComment.toByteArray());
        assertThat(new String(image.bytes(), StandardCharsets.ISO_8859_1)).doesNotContain("GPS 52.0907");
    }

    @Test
    void refusesOtherFormatsGarbageAndTinyImages() {
        assertThatThrownBy(() -> ImageProcessor.process(TestImages.gif(100, 100)))
                .isInstanceOf(InvalidImageException.class).hasMessageContaining("JPEG and PNG");
        assertThatThrownBy(() -> ImageProcessor.process("<svg/>".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(InvalidImageException.class);
        assertThatThrownBy(() -> ImageProcessor.process(TestImages.png(10, 10, false)))
                .isInstanceOf(InvalidImageException.class).hasMessageContaining("at least");
    }

    @Test
    void refusesAnImageThatClaimsTooManyPixelsBeforeDecodingIt() {
        byte[] png = TestImages.png(64, 64, false);
        // IHDR width and height are bytes 16..23 of a PNG; claim 20000 x 20000 pixels
        byte[] forged = png.clone();
        writeInt(forged, 16, 20_000);
        writeInt(forged, 20, 20_000);
        assertThatThrownBy(() -> ImageProcessor.process(forged)).isInstanceOf(InvalidImageException.class);
    }

    private static void writeInt(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) (value >>> 24);
        bytes[offset + 1] = (byte) (value >>> 16);
        bytes[offset + 2] = (byte) (value >>> 8);
        bytes[offset + 3] = (byte) value;
    }
}
