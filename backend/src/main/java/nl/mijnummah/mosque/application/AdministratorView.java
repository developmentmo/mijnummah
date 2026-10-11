package nl.mijnummah.mosque.application;

import java.time.Instant;
import java.util.UUID;

import nl.mijnummah.mosque.MosqueRole;

public record AdministratorView(UUID id, UUID userId, String email, String displayName, MosqueRole role,
                                Instant createdAt) {
}
