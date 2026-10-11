package nl.mijnummah.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PasswordResetCodeTest {

    private static final Instant ISSUED = Instant.parse("2026-10-11T10:00:00Z");

    private final PasswordResetCode code =
            PasswordResetCode.issue(UUID.randomUUID(), "hash", ISSUED, Duration.ofMinutes(30));

    @Test
    void worksUntilItExpires() {
        assertThat(code.isUsable(ISSUED.plus(Duration.ofMinutes(29)))).isTrue();
        assertThat(code.isUsable(ISSUED.plus(Duration.ofMinutes(30)))).isFalse();
    }

    @Test
    void stopsWorkingOnceUsedOrAfterTheLastWrongAttempt() {
        code.recordFailedAttempt(2, ISSUED);
        assertThat(code.isUsable(ISSUED)).isTrue();
        code.recordFailedAttempt(2, ISSUED);
        assertThat(code.isUsable(ISSUED)).isFalse();

        PasswordResetCode used = PasswordResetCode.issue(UUID.randomUUID(), "hash", ISSUED, Duration.ofMinutes(30));
        used.markUsed(ISSUED);
        assertThat(used.isUsable(ISSUED)).isFalse();
    }

    @Test
    void comparesHashes() {
        assertThat(code.matches("hash")).isTrue();
        assertThat(code.matches("other")).isFalse();
    }
}
