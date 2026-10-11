package nl.mijnummah.notification.infrastructure;

import java.net.URI;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import nl.mijnummah.notification.application.DeliveryResult;
import nl.mijnummah.notification.application.PushGateway;
import nl.mijnummah.notification.application.PushMessage;
import nl.mijnummah.notification.application.PushServiceException;

/**
 * Firebase Cloud Messaging, HTTP v1 API: one request per device token. FCM delivers to Android directly and to iOS
 * through APNs (the APNs key is configured in the Firebase project, not here).
 */
class FcmPushGateway implements PushGateway {

    private static final Logger log = LoggerFactory.getLogger(FcmPushGateway.class);

    private final RestClient http;
    private final URI sendUri;
    private final ServiceAccountTokens tokens;

    FcmPushGateway(RestClient http, URI apiBaseUrl, String projectId, ServiceAccountTokens tokens) {
        this.http = http;
        this.sendUri = apiBaseUrl.resolve("/v1/projects/" + projectId + "/messages:send");
        this.tokens = tokens;
    }

    @Override
    public boolean configured() {
        return true;
    }

    @Override
    public DeliveryResult send(PushMessage message, String token) {
        Map<String, Object> body = Map.of("message", Map.of(
                "token", token,
                "notification", Map.of("title", message.title(), "body", message.body()),
                "data", message.data(),
                "android", Map.of("priority", "HIGH"),
                "apns", Map.of("payload", Map.of("aps", Map.of("sound", "default")))));
        try {
            http.post().uri(sendUri)
                    .header("Authorization", "Bearer " + tokens.get())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return DeliveryResult.DELIVERED;
        } catch (RestClientResponseException ex) {
            return classify(ex.getStatusCode(), ex.getResponseBodyAsString());
        } catch (PushServiceException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.warn("FCM not reachable: {}", ex.getMessage());
            return DeliveryResult.FAILED;
        }
    }

    /**
     * 404 (UNREGISTERED) and a 400 about the registration token mean the token is dead. 401/403 mean our credentials
     * or project setup are wrong, which affects every device. Anything else is temporary.
     */
    static DeliveryResult classify(HttpStatusCode status, String responseBody) {
        int code = status.value();
        if (code == 404 || (code == 400 && responseBody != null && responseBody.contains("registration token"))) {
            return DeliveryResult.INVALID_TOKEN;
        }
        if (code == 401 || code == 403) {
            throw new PushServiceException("FCM rejected the credentials (HTTP " + code + ")");
        }
        log.warn("FCM did not accept a message (HTTP {})", code);
        return DeliveryResult.FAILED;
    }
}
