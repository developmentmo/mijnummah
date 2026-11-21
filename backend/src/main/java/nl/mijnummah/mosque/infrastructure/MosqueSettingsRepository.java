package nl.mijnummah.mosque.infrastructure;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import nl.mijnummah.mosque.domain.MosqueSettings;

public interface MosqueSettingsRepository extends JpaRepository<MosqueSettings, UUID> {
}
