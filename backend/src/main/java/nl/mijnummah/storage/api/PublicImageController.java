package nl.mijnummah.storage.api;

import java.time.Duration;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import nl.mijnummah.storage.ImageNotFoundException;
import nl.mijnummah.storage.application.ImageStorageService;

/**
 * Serves uploaded images to the app and the portal. An image never changes after upload (a new upload gets a new
 * ID), so clients may cache it for a year. IDs are random UUIDs that only appear in responses about the content the
 * image belongs to.
 */
@RestController
@RequestMapping("/api/v1/public/images")
@Tag(name = "Publiek: afbeeldingen")
class PublicImageController {

    private final ImageStorageService images;

    PublicImageController(ImageStorageService images) {
        this.images = images;
    }

    @GetMapping("/{imageId}")
    @Operation(summary = "Afbeelding (JPEG of PNG)")
    ResponseEntity<byte[]> get(@PathVariable UUID imageId) {
        var image = images.load(imageId).orElseThrow(ImageNotFoundException::new);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .body(image.bytes());
    }
}
