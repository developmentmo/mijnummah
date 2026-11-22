package nl.mijnummah.dashboard.application;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.mosque.MosqueAccessService;
import nl.mijnummah.mosque.MosquePermission;

/** Figures for the dashboard of one mosque, for its administrators and editors (read-only SQL, see D-058). */
@Service
public class MosqueStatistics {

    private static final Duration RECENT = Duration.ofDays(30);

    private final JdbcTemplate jdbc;
    private final MosqueAccessService access;
    private final Clock clock;

    public MosqueStatistics(JdbcTemplate jdbc, MosqueAccessService access, Clock clock) {
        this.jdbc = jdbc;
        this.access = access;
        this.clock = clock;
    }

    /**
     * @param followers              people who follow the mosque
     * @param newFollowers           of which in the last 30 days
     * @param upcomingEvents         published events that have not ended
     * @param draftEvents            events not published yet
     * @param publishedVideos        videos visible in the app
     * @param scheduledVideos        published videos whose publication date is still ahead
     * @param draftVideos            videos not published yet
     * @param publishedAnnouncements announcements visible in the app (published and not expired)
     * @param draftAnnouncements     announcements not published yet
     * @param recentPushNotifications push notifications sent in the last 30 days
     */
    public record Figures(long followers, long newFollowers, long upcomingEvents, long draftEvents,
                          long publishedVideos, long scheduledVideos, long draftVideos, long publishedAnnouncements,
                          long draftAnnouncements, long recentPushNotifications) {
    }

    @Transactional(readOnly = true)
    public Figures figures(AuthenticatedUser actor, UUID mosqueId) {
        access.require(actor, mosqueId, MosquePermission.MANAGE_CONTENT);
        Instant nowInstant = Instant.now(clock);
        Timestamp now = Timestamp.from(nowInstant);
        Timestamp since = Timestamp.from(nowInstant.minus(RECENT));
        return jdbc.queryForObject("""
                select
                  (select count(*) from mosque_follow where mosque_id = ?) as followers,
                  (select count(*) from mosque_follow where mosque_id = ? and created_at >= ?) as new_followers,
                  (select count(*) from event where mosque_id = ? and status = 'PUBLISHED'
                     and coalesce(ends_at, starts_at) > ?) as upcoming_events,
                  (select count(*) from event where mosque_id = ? and status = 'DRAFT') as draft_events,
                  (select count(*) from media_item where mosque_id = ? and status = 'PUBLISHED'
                     and published_at <= ?) as published_videos,
                  (select count(*) from media_item where mosque_id = ? and status = 'PUBLISHED'
                     and published_at > ?) as scheduled_videos,
                  (select count(*) from media_item where mosque_id = ? and status = 'DRAFT') as draft_videos,
                  (select count(*) from announcement where mosque_id = ? and status = 'PUBLISHED'
                     and (expires_at is null or expires_at > ?)) as published_announcements,
                  (select count(*) from announcement where mosque_id = ? and status = 'DRAFT') as draft_announcements,
                  (select count(*) from push_notification where mosque_id = ? and created_at >= ?) as recent_push
                """,
                (rs, row) -> new Figures(rs.getLong("followers"), rs.getLong("new_followers"),
                        rs.getLong("upcoming_events"), rs.getLong("draft_events"), rs.getLong("published_videos"),
                        rs.getLong("scheduled_videos"), rs.getLong("draft_videos"),
                        rs.getLong("published_announcements"), rs.getLong("draft_announcements"),
                        rs.getLong("recent_push")),
                mosqueId, mosqueId, since, mosqueId, now, mosqueId, mosqueId, now, mosqueId, now, mosqueId, mosqueId,
                now, mosqueId, mosqueId, since);
    }
}
