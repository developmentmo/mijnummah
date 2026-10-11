package nl.mijnummah.mosque.application;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.mosque.MosqueDirectory;
import nl.mijnummah.mosque.MosqueRef;
import nl.mijnummah.mosque.domain.Mosque;
import nl.mijnummah.mosque.infrastructure.MosqueRepository;

@Service
class MosqueDirectoryService implements MosqueDirectory {

    private final MosqueRepository mosques;

    MosqueDirectoryService(MosqueRepository mosques) {
        this.mosques = mosques;
    }

    @Override
    @Transactional(readOnly = true)
    public MosqueRef requireActiveBySlug(String slug) {
        return mosques.findBySlug(slug).filter(Mosque::isActive).map(MosqueDirectoryService::ref)
                .orElseThrow(MosqueNotFound::exception);
    }

    @Override
    @Transactional(readOnly = true)
    public MosqueRef requireActive(UUID mosqueId) {
        return mosques.findById(mosqueId).filter(Mosque::isActive).map(MosqueDirectoryService::ref)
                .orElseThrow(MosqueNotFound::exception);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, MosqueRef> findActiveByIds(Collection<UUID> mosqueIds) {
        return mosques.findAllById(mosqueIds).stream()
                .filter(Mosque::isActive)
                .map(MosqueDirectoryService::ref)
                .collect(Collectors.toUnmodifiableMap(MosqueRef::id, Function.identity()));
    }

    static MosqueRef ref(Mosque mosque) {
        return new MosqueRef(mosque.getId(), mosque.getSlug(), mosque.getName(), mosque.getTimeZone());
    }
}
