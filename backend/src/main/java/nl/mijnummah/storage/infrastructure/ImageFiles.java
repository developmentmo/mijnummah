package nl.mijnummah.storage.infrastructure;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * The image files, in a directory: {@code <directory>/<first two characters of the ID>/<ID>}. A file is written to
 * a temporary name first and then moved, so a reader never sees half a file.
 */
@Component
@EnableConfigurationProperties(StorageProperties.class)
public class ImageFiles {

    private final Path root;

    ImageFiles(StorageProperties properties) {
        this.root = properties.directory().toAbsolutePath().normalize();
    }

    public void write(UUID id, byte[] bytes) {
        Path target = path(id);
        try {
            Files.createDirectories(target.getParent());
            Path temporary = Files.createTempFile(target.getParent(), id.toString(), ".tmp");
            try {
                Files.write(temporary, bytes);
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not store image " + id, ex);
        }
    }

    public Optional<byte[]> read(UUID id) {
        Path file = path(id);
        try {
            return Files.isRegularFile(file) ? Optional.of(Files.readAllBytes(file)) : Optional.empty();
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not read image " + id, ex);
        }
    }

    private Path path(UUID id) {
        String name = id.toString();
        return root.resolve(name.substring(0, 2)).resolve(name);
    }
}
