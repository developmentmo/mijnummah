package nl.mijnummah.storage.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.storage.ImageNotFoundException;
import nl.mijnummah.storage.ImageStorage;
import nl.mijnummah.storage.StoredImageRef;
import nl.mijnummah.storage.domain.EncodedImage;
import nl.mijnummah.storage.domain.ImageProcessor;
import nl.mijnummah.storage.domain.StoredImage;
import nl.mijnummah.storage.infrastructure.ImageFiles;
import nl.mijnummah.storage.infrastructure.StoredImageRepository;

@Service
public class ImageStorageService implements ImageStorage {

    private final StoredImageRepository images;
    private final ImageFiles files;
    private final Clock clock;

    ImageStorageService(StoredImageRepository images, ImageFiles files, Clock clock) {
        this.images = images;
        this.files = files;
        this.clock = clock;
    }

    /** The row is saved first and the file written last: when writing fails, the row is rolled back with it. */
    @Override
    @Transactional
    public StoredImageRef store(UUID mosqueId, UUID uploaderId, byte[] content) {
        EncodedImage encoded = ImageProcessor.process(content);
        StoredImage image = images.saveAndFlush(StoredImage.of(mosqueId, uploaderId, encoded, Instant.now(clock)));
        files.write(image.getId(), encoded.bytes());
        return new StoredImageRef(image.getId(), ImageStorage.publicPath(image.getId()), image.getWidth(),
                image.getHeight());
    }

    @Override
    @Transactional(readOnly = true)
    public void requireOwnedBy(UUID imageId, UUID mosqueId) {
        if (!images.existsByIdAndMosqueId(imageId, mosqueId)) {
            throw new ImageNotFoundException();
        }
    }

    /** The file and its type, for serving. */
    @Transactional(readOnly = true)
    public Optional<ImageContent> load(UUID imageId) {
        return images.findById(imageId)
                .flatMap(image -> files.read(imageId).map(bytes -> new ImageContent(image.getContentType(), bytes)));
    }

    public record ImageContent(String contentType, byte[] bytes) {
    }
}
