package nl.mijnummah.notification.infrastructure;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from {@code mijnummah.push.*}.
 *
 * @param provider {@code none} (default: sending is not available), {@code fcm} (Firebase Cloud Messaging) or
 *                 {@code log} (local development only: messages are written to the log, nothing is sent)
 * @param fcm      settings for {@code fcm}
 */
@ConfigurationProperties(prefix = "mijnummah.push")
public record PushProperties(Provider provider, Fcm fcm) {

    public enum Provider { NONE, FCM, LOG }

    public PushProperties {
        provider = provider == null ? Provider.NONE : provider;
        fcm = fcm == null ? new Fcm(null, null, null) : fcm;
        if (provider == Provider.FCM) {
            fcm.requireComplete();
        }
    }

    /**
     * @param projectId       the Firebase project ID
     * @param credentialsFile path to the service account key (JSON) of that project, mounted as a secret; never in
     *                        the repository
     * @param apiBaseUrl      FCM HTTP v1 API, only overridden by tests
     */
    public record Fcm(String projectId, String credentialsFile, URI apiBaseUrl) {

        public Fcm {
            apiBaseUrl = apiBaseUrl == null ? URI.create("https://fcm.googleapis.com") : apiBaseUrl;
        }

        void requireComplete() {
            if (projectId == null || projectId.isBlank()) {
                throw new IllegalStateException("PUSH_PROVIDER=fcm needs mijnummah.push.fcm.project-id (FCM_PROJECT_ID)");
            }
            if (credentialsFile == null || credentialsFile.isBlank()) {
                throw new IllegalStateException(
                        "PUSH_PROVIDER=fcm needs mijnummah.push.fcm.credentials-file (FCM_CREDENTIALS_FILE)");
            }
        }
    }
}
