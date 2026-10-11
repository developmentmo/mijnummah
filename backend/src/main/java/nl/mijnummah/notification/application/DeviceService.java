package nl.mijnummah.notification.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.notification.domain.DevicePlatform;
import nl.mijnummah.notification.infrastructure.PushDeviceRepository;

/** Push tokens of the caller's own devices. */
@Service
public class DeviceService {

    private final PushDeviceRepository devices;
    private final Clock clock;

    public DeviceService(PushDeviceRepository devices, Clock clock) {
        this.devices = devices;
        this.clock = clock;
    }

    /**
     * Registers the token for the caller (idempotent). A token registered earlier by someone else moves to the
     * caller: one device belongs to whoever is signed in on it.
     */
    @Transactional
    public void register(AuthenticatedUser actor, String token, DevicePlatform platform) {
        devices.upsert(UUID.randomUUID(), actor.id(), token, platform.name(), Instant.now(clock));
    }

    /** Removes the token if it is the caller's (at sign-out, or when notifications are switched off). Idempotent. */
    @Transactional
    public void unregister(AuthenticatedUser actor, String token) {
        devices.deleteByTokenAndUserId(token, actor.id());
    }
}
