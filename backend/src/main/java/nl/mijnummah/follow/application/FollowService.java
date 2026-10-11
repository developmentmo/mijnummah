package nl.mijnummah.follow.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.common.error.NotFoundException;
import nl.mijnummah.follow.domain.MosqueFollow;
import nl.mijnummah.follow.domain.NotificationPreference;
import nl.mijnummah.follow.domain.NotificationTopic;
import nl.mijnummah.follow.infrastructure.MosqueFollowRepository;
import nl.mijnummah.follow.infrastructure.NotificationPreferenceRepository;
import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.mosque.MosqueDirectory;
import nl.mijnummah.mosque.MosqueRef;

/** Follow and unfollow mosques, and per-topic notification preferences. Always about the caller's own data. */
@Service
public class FollowService {

    private final MosqueFollowRepository follows;
    private final NotificationPreferenceRepository preferences;
    private final MosqueDirectory mosques;
    private final Clock clock;

    public FollowService(MosqueFollowRepository follows, NotificationPreferenceRepository preferences,
                         MosqueDirectory mosques, Clock clock) {
        this.follows = follows;
        this.preferences = preferences;
        this.mosques = mosques;
        this.clock = clock;
    }

    /** Followed mosques that are active, oldest follow first. */
    @Transactional(readOnly = true)
    public List<FollowView> mine(AuthenticatedUser actor) {
        List<MosqueFollow> rows = follows.findByUserIdOrderByCreatedAt(actor.id());
        Map<UUID, MosqueRef> active = mosques.findActiveByIds(rows.stream().map(MosqueFollow::getMosqueId).toList());
        Map<UUID, Map<NotificationTopic, Boolean>> prefs = preferencesOf(actor.id(), active.keySet().stream().toList());
        return rows.stream()
                .filter(row -> active.containsKey(row.getMosqueId()))
                .map(row -> view(row, active.get(row.getMosqueId()), prefs.get(row.getMosqueId())))
                .toList();
    }

    /** Idempotent: following a mosque twice keeps the first follow and its preferences. */
    @Transactional
    public FollowView follow(AuthenticatedUser actor, UUID mosqueId) {
        MosqueRef mosque = mosques.requireActive(mosqueId);
        MosqueFollow follow = follows.findById(new MosqueFollow.Key(actor.id(), mosqueId))
                .orElseGet(() -> follows.save(MosqueFollow.start(actor.id(), mosqueId, Instant.now(clock))));
        return view(follow, mosque, preferencesOf(actor.id(), List.of(mosqueId)).get(mosqueId));
    }

    /** Idempotent. Removes the notification preferences too (database cascade). */
    @Transactional
    public void unfollow(AuthenticatedUser actor, UUID mosqueId) {
        follows.findById(new MosqueFollow.Key(actor.id(), mosqueId)).ifPresent(follows::delete);
    }

    @Transactional(readOnly = true)
    public FollowView get(AuthenticatedUser actor, UUID mosqueId) {
        MosqueRef mosque = mosques.requireActive(mosqueId);
        MosqueFollow follow = requireFollow(actor, mosqueId);
        return view(follow, mosque, preferencesOf(actor.id(), List.of(mosqueId)).get(mosqueId));
    }

    /** Changes only the topics in {@code changes}; others keep their current value. */
    @Transactional
    public FollowView updatePreferences(AuthenticatedUser actor, UUID mosqueId,
                                        Map<NotificationTopic, Boolean> changes) {
        MosqueRef mosque = mosques.requireActive(mosqueId);
        MosqueFollow follow = requireFollow(actor, mosqueId);
        Instant now = Instant.now(clock);
        changes.forEach((topic, enabled) -> {
            var key = new NotificationPreference.Key(actor.id(), mosqueId, topic);
            preferences.findById(key).ifPresentOrElse(
                    existing -> existing.change(enabled, now),
                    () -> preferences.save(NotificationPreference.of(actor.id(), mosqueId, topic, enabled, now)));
        });
        preferences.flush();
        return view(follow, mosque, preferencesOf(actor.id(), List.of(mosqueId)).get(mosqueId));
    }

    private MosqueFollow requireFollow(AuthenticatedUser actor, UUID mosqueId) {
        return follows.findById(new MosqueFollow.Key(actor.id(), mosqueId))
                .orElseThrow(() -> new NotFoundException("NOT_FOLLOWING", "You do not follow this mosque."));
    }

    /** Per mosque, every topic with its effective value (stored choice, or enabled by default). */
    private Map<UUID, Map<NotificationTopic, Boolean>> preferencesOf(UUID userId, List<UUID> mosqueIds) {
        Map<UUID, Map<NotificationTopic, Boolean>> result = new HashMap<>();
        for (UUID mosqueId : mosqueIds) {
            Map<NotificationTopic, Boolean> defaults = new EnumMap<>(NotificationTopic.class);
            for (NotificationTopic topic : NotificationTopic.values()) {
                defaults.put(topic, true);
            }
            result.put(mosqueId, defaults);
        }
        if (!mosqueIds.isEmpty()) {
            for (NotificationPreference preference : preferences.findByUserIdAndMosqueIdIn(userId, mosqueIds)) {
                result.get(preference.getMosqueId()).put(preference.getTopic(), preference.isEnabled());
            }
        }
        return result;
    }

    private static FollowView view(MosqueFollow follow, MosqueRef mosque, Map<NotificationTopic, Boolean> prefs) {
        return new FollowView(mosque.id(), mosque.slug(), mosque.name(), follow.getCreatedAt(),
                Collections.unmodifiableMap(new EnumMap<>(prefs)));
    }
}
