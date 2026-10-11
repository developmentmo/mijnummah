package nl.mijnummah.mosque.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import nl.mijnummah.mosque.domain.Mosque;

public interface MosqueRepository extends JpaRepository<Mosque, UUID> {

    Optional<Mosque> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
