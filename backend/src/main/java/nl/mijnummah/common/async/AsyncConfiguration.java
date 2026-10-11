package nl.mijnummah.common.async;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables {@code @Async} for work that must not delay or influence the HTTP response: sending emails (so a password
 * reset request takes equally long for known and unknown addresses) and sending push notifications. Uses Boot's
 * auto-configured task executor ({@code spring.task.execution.*}).
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class AsyncConfiguration {
}
