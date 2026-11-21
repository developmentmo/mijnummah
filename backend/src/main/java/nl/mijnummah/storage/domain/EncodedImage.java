package nl.mijnummah.storage.domain;

/** An image after re-encoding: the bytes to store and their type and size. */
public record EncodedImage(byte[] bytes, String contentType, int width, int height) {
}
