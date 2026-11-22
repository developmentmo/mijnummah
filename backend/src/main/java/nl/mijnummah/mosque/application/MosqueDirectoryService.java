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
import nl.mijnummah.mosque.MosqueSettingsView;
import nl.mijnummah.mosque.domain.Mosque;
import nl.mijnummah.mosque.infrastructure.MosqueRepository;
import nl.mijnummah.mosque.infrastructure.MosqueSettingsRepository;

@Service
class MosqueDirectoryService implements MosqueDirectory {

    private final MosqueRepository mosques;
    private final MosqueSettingsRepository settings;

    MosqueDirectoryService(MosqueRepository mosques, MosqueSettingsRepository settings) {
        this.mosques = mosques;
        this.settings = settings;
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

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> namesByIds(Collection<UUID> mosqueIds) {
        return mosqueIds.isEmpty() ? Map.of() : mosques.findAllById(mosqueIds).stream()
                .collect(Collectors.toUnmodifiableMap(Mosque::getId, Mosque::getName));
    }

    @Override
    @Transactional(readOnly = true)
    public MosqueSettingsView settings(UUID mosqueId) {
        return settings.findById(mosqueId).map(MosqueService::settingsView).orElse(MosqueSettingsView.DEFAULTS);
    }

    static MosqueRef ref(Mosque mosque) {
        return new MosqueRef(mosque.getId(), mosque.getSlug(), mosque.getName(), mosque.getTimeZone());
    }
}
