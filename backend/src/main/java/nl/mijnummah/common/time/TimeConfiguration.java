package nl.mijnummah.common.time;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Single UTC time source for the application.
 * <p>
 * Inject {@link Clock} instead of calling {@code Instant.now()} / {@code LocalDate.now()} directly,
 * so time-dependent rules (e.g. "only upcoming events", "announcement expired") are deterministic in tests.
 */
@Configuration(proxyBeanMethods = false)
public class TimeConfiguration {

    /**
     * Ticks in microseconds, the precision PostgreSQL stores: an instant returned right after a save is then equal
     * to the same instant read back later. ({@code Clock.tick} cannot be used: its {@code millis()} divides by zero
     * for ticks below one millisecond.)
     */
    @Bean
    Clock clock() {
        return new MicrosecondClock(Clock.systemUTC());
    }

    static final class MicrosecondClock extends Clock {

        private final Clock delegate;

        MicrosecondClock(Clock delegate) {
            this.delegate = delegate;
        }

        @Override
        public ZoneId getZone() {
            return delegate.getZone();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MicrosecondClock(delegate.withZone(zone));
        }

        @Override
        public Instant instant() {
            return delegate.instant().truncatedTo(ChronoUnit.MICROS);
        }

        @Override
        public long millis() {
            return delegate.millis();
        }
    }
}
