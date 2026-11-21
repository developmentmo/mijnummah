package nl.mijnummah.dashboard.application;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.mosque.MosqueAccessDeniedException;
import nl.mijnummah.mosque.MosqueStatus;

/** Platform administrators only. */
@Service
public class PlatformStatistics {

    private static final Duration RECENT = Duration.ofDays(30);

    private final JdbcTemplate jdbc;
    private final Clock clock;

    public PlatformStatistics(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Totals totals(AuthenticatedUser actor) {
        requirePlatformAdmin(actor);
        Instant now = Instant.now(clock);
        Timestamp since = Timestamp.from(now.minus(RECENT));
        return jdbc.queryForObject("""
                select
                  (select count(*) from mosque) as mosques,
                  (select count(*) from mosque where status = 'ACTIVE') as active_mosques,
                  (select count(*) from mosque where status = 'INACTIVE') as inactive_mosques,
                  (select count(*) from mosque where status = 'BLOCKED') as blocked_mosques,
                  (select count(*) from app_user) as users,
                  (select count(*) from app_user where created_at >= ?) as new_users,
                  (select count(distinct user_id) from mosque_follow) as followers,
                  (select count(*) from mosque_follow) as follows,
                  (select count(*) from event) as events,
                  (select count(*) from event where status = 'PUBLISHED' and starts_at >= ?) as upcoming_events,
                  (select count(*) from media_item where status = 'PUBLISHED') as published_videos,
                  (select count(*) from announcement where status = 'PUBLISHED') as published_announcements,
                  (select count(*) from push_notification where created_at >= ?) as recent_push_notifications
                """, (rs, row) -> new Totals(
                new Totals.Mosques(rs.getLong("mosques"), rs.getLong("active_mosques"),
                        rs.getLong("inactive_mosques"), rs.getLong("blocked_mosques")),
                rs.getLong("users"), rs.getLong("new_users"), rs.getLong("followers"), rs.getLong("follows"),
                rs.getLong("events"), rs.getLong("upcoming_events"), rs.getLong("published_videos"),
                rs.getLong("published_announcements"), rs.getLong("recent_push_notifications")),
                since, Timestamp.from(now), since);
    }

    /** Every mosque with its content counts, by name. */
    @Transactional(readOnly = true)
    public List<MosqueCounts> perMosque(AuthenticatedUser actor) {
        requirePlatformAdmin(actor);
        return jdbc.query("""
                select m.id, m.slug, m.name, m.city, m.status, m.created_at,
                  (select count(*) from mosque_administrator a where a.mosque_id = m.id) as administrators,
                  (select count(*) from mosque_follow f where f.mosque_id = m.id) as followers,
                  (select count(*) from event e where e.mosque_id = m.id) as events,
                  (select count(*) from media_item v where v.mosque_id = m.id and v.status = 'PUBLISHED') as videos,
                  (select count(*) from announcement n where n.mosque_id = m.id and n.status = 'PUBLISHED')
                    as announcements
                from mosque m
                order by lower(m.name), m.id
                """, (rs, row) -> new MosqueCounts(rs.getObject("id", UUID.class), rs.getString("slug"),
                rs.getString("name"), rs.getString("city"), MosqueStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(), rs.getLong("administrators"), rs.getLong("followers"),
                rs.getLong("events"), rs.getLong("videos"), rs.getLong("announcements")));
    }

    private static void requirePlatformAdmin(AuthenticatedUser actor) {
        if (!actor.isPlatformAdmin()) {
            throw new MosqueAccessDeniedException();
        }
    }

    /**
     * @param newUsers                accounts created in the last 30 days
     * @param followers               people who follow at least one mosque
     * @param follows                 follow relations (one person can follow several mosques)
     * @param events                  all events, also drafts and cancelled ones
     * @param recentPushNotifications sent in the last 30 days
     */
    public record Totals(Mosques mosques, long users, long newUsers, long followers, long follows, long events,
                         long upcomingEvents, long publishedVideos, long publishedAnnouncements,
                         long recentPushNotifications) {

        public record Mosques(long total, long active, long inactive, long blocked) {
        }
    }

    /** @param events all events of the mosque; videos and announcements: published ones */
    public record MosqueCounts(UUID id, String slug, String name, String city, MosqueStatus status,
                               Instant createdAt, long administrators, long followers, long events, long videos,
                               long announcements) {
    }
}
