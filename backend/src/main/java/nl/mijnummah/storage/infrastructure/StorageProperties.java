package nl.mijnummah.storage.infrastructure;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Where uploaded images are stored, bound from {@code mijnummah.storage.*}. One directory on the server (a Docker
 * volume in production, included in the backup). An S3-compatible store can replace it behind
 * {@link ImageFiles} when one server is no longer enough.
 *
 * @param directory directory for the image files; created when missing
 */
@ConfigurationProperties(prefix = "mijnummah.storage")
public record StorageProperties(Path directory) {

    public StorageProperties {
        if (directory == null) {
            throw new IllegalStateException("mijnummah.storage.directory is required");
        }
    }
}
