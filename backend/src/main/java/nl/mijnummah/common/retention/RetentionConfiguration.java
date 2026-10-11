package nl.mijnummah.common.retention;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on the scheduled clean-up jobs of the modules ({@code *Retention} classes). With
 * {@code mijnummah.retention.enabled=false} nothing is scheduled; the clean-up methods can still be called directly.
 * <p>
 * Every job is an idempotent delete or update, so running more than one backend instance is safe: an instance that
 * runs later simply finds nothing left to do.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RetentionProperties.class)
public class RetentionConfiguration {

    /** Zone of the nightly schedule, so it runs at night for the mosque, also in summer time. */
    public static final String ZONE = "Europe/Amsterdam";

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    @ConditionalOnProperty(prefix = "mijnummah.retention", name = "enabled", havingValue = "true",
            matchIfMissing = true)
    static class Scheduling {
    }
}
