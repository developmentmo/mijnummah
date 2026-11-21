package nl.mijnummah.storage.infrastructure;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import nl.mijnummah.storage.domain.StoredImage;

public interface StoredImageRepository extends JpaRepository<StoredImage, UUID> {

    boolean existsByIdAndMosqueId(UUID id, UUID mosqueId);
}
