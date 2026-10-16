package nl.mijnummah.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import nl.mijnummah.AbstractIntegrationTest;

/**
 * The database itself enforces the rules that matter most, so a bug in the application cannot break them:
 * reproducible migrations, tenant consistency of references, no duplicate registrations, valid periods.
 */
class DatabaseConstraintsIT extends AbstractIntegrationTest {

    private UUID mosqueA;
    private UUID mosqueB;
    private UUID eventA;
    private UUID userId;

    @BeforeEach
    void data() {
        mosqueA = mosque("moskee-a");
        mosqueB = mosque("moskee-b");
        Account admin = mosqueAdmin(mosqueA, "admin");
        eventA = publishedEvent(mosqueA, admin.token());
        userId = admin.id();
    }

    @Test
    void allMigrationsAreAppliedInOrder() {
        Integer applied = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success and version is not null", Integer.class);
        String latest = jdbc.queryForObject(
                "select version from flyway_schema_history order by installed_rank desc limit 1", String.class);
        assertThat(applied).isEqualTo(13);
        assertThat(latest).isEqualTo("13");
    }

    @Test
    void aRegistrationCannotPointToAnEventOfAnotherMosque() {
        assertThatThrownBy(() -> jdbc.update("""
                insert into event_registration (id, mosque_id, event_id, user_id, status, created_at, updated_at, version)
                values (?, ?, ?, ?, 'CONFIRMED', now(), now(), 0)""", UUID.randomUUID(), mosqueB, eventA, userId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void aUserCannotBeRegisteredTwiceForTheSameEvent() {
        String insert = """
                insert into event_registration (id, mosque_id, event_id, user_id, status, created_at, updated_at, version)
                values (?, ?, ?, ?, 'CONFIRMED', now(), now(), 0)""";
        jdbc.update(insert, UUID.randomUUID(), mosqueA, eventA, userId);
        assertThatThrownBy(() -> jdbc.update(insert, UUID.randomUUID(), mosqueA, eventA, userId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void anEventCannotEndBeforeItStarts() {
        assertThatThrownBy(() -> jdbc.update("update event set ends_at = starts_at - interval '1 hour' where id = ?",
                eventA)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void mediaCannotLinkToAnEventOfAnotherMosque() {
        assertThatThrownBy(() -> jdbc.update("""
                insert into media_item (id, mosque_id, title, category, video_provider, video_id, related_event_id,
                                        visibility, status, created_at, created_by, updated_at, updated_by, version)
                values (?, ?, 'X', 'LECTURE', 'YOUTUBE', 'aaaaaaaaaaa', ?, 'PUBLIC', 'DRAFT', now(), ?, now(), ?, 0)""",
                UUID.randomUUID(), mosqueB, eventA, userId, userId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void emailsAreStoredLowerCaseAndUnique() {
        assertThatThrownBy(() -> jdbc.update("update app_user set email = upper(email) where id = ?", userId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void noVideoContentColumnsExist() {
        Integer binaryColumns = jdbc.queryForObject(
                "select count(*) from information_schema.columns where table_schema = 'public' and data_type = 'bytea'",
                Integer.class);
        assertThat(binaryColumns).isZero();
    }
}
