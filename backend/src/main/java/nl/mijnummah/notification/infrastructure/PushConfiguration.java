package nl.mijnummah.notification.infrastructure;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.json.JsonMapper;

import nl.mijnummah.common.profile.LocalOnly;
import nl.mijnummah.notification.application.DeliveryResult;
import nl.mijnummah.notification.application.PushGateway;
import nl.mijnummah.notification.application.PushMessage;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PushProperties.class)
class PushConfiguration {

    private static final Logger log = LoggerFactory.getLogger(PushConfiguration.class);

    @Bean
    PushGateway pushGateway(PushProperties properties, JsonMapper json, Clock clock, Environment environment) {
        return switch (properties.provider()) {
            case NONE -> new UnconfiguredPushGateway();
            case LOG -> {
                LocalOnly.require(environment, "PUSH_PROVIDER=log");
                log.warn("PUSH_PROVIDER=log: push notifications are written to the log and NOT sent. "
                        + "Local development only.");
                yield new LoggingPushGateway();
            }
            case FCM -> {
                RestClient http = restClient();
                var fcm = properties.fcm();
                var tokens = ServiceAccountTokens.fromFile(Path.of(fcm.credentialsFile()), json, http, clock);
                yield new FcmPushGateway(http, fcm.apiBaseUrl(), fcm.projectId(), tokens);
            }
        };
    }

    private static RestClient restClient() {
        var factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().requestFactory(factory).build();
    }

    static final class UnconfiguredPushGateway implements PushGateway {

        @Override
        public boolean configured() {
            return false;
        }

        @Override
        public DeliveryResult send(PushMessage message, String token) {
            throw new IllegalStateException("No push service configured (PUSH_PROVIDER=none)");
        }
    }

    static final class LoggingPushGateway implements PushGateway {

        @Override
        public boolean configured() {
            return true;
        }

        @Override
        public boolean simulated() {
            return true;
        }

        @Override
        public DeliveryResult send(PushMessage message, String token) {
            log.info("[PUSH_PROVIDER=log, not sent] to device …{}: {} | {} | {}",
                    token.substring(Math.max(0, token.length() - 6)), message.title(), message.body(), message.data());
            return DeliveryResult.DELIVERED;
        }
    }
}
