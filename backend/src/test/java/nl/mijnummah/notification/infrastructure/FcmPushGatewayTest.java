package nl.mijnummah.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;

import nl.mijnummah.notification.application.DeliveryResult;
import nl.mijnummah.notification.application.PushServiceException;

class FcmPushGatewayTest {

    @Test
    void goneAppsAreInvalidTokens() {
        assertThat(FcmPushGateway.classify(HttpStatusCode.valueOf(404), "{\"error\":{\"status\":\"NOT_FOUND\"}}"))
                .isEqualTo(DeliveryResult.INVALID_TOKEN);
        assertThat(FcmPushGateway.classify(HttpStatusCode.valueOf(400),
                "{\"error\":{\"message\":\"The registration token is not a valid FCM registration token\"}}"))
                .isEqualTo(DeliveryResult.INVALID_TOKEN);
    }

    @Test
    void otherBadRequestsAndOutagesKeepTheDevice() {
        assertThat(FcmPushGateway.classify(HttpStatusCode.valueOf(400), "{\"error\":{\"message\":\"bad payload\"}}"))
                .isEqualTo(DeliveryResult.FAILED);
        assertThat(FcmPushGateway.classify(HttpStatusCode.valueOf(429), "")).isEqualTo(DeliveryResult.FAILED);
        assertThat(FcmPushGateway.classify(HttpStatusCode.valueOf(503), null)).isEqualTo(DeliveryResult.FAILED);
    }

    @Test
    void rejectedCredentialsStopTheWholeNotification() {
        assertThatThrownBy(() -> FcmPushGateway.classify(HttpStatusCode.valueOf(401), ""))
                .isInstanceOf(PushServiceException.class);
        assertThatThrownBy(() -> FcmPushGateway.classify(HttpStatusCode.valueOf(403), ""))
                .isInstanceOf(PushServiceException.class);
    }

    @Test
    void fcmNeedsAProjectAndCredentials() {
        assertThatThrownBy(() -> new PushProperties(PushProperties.Provider.FCM, null))
                .hasMessageContaining("FCM_PROJECT_ID");
        assertThatThrownBy(() -> new PushProperties(PushProperties.Provider.FCM,
                new PushProperties.Fcm("project", " ", null)))
                .hasMessageContaining("FCM_CREDENTIALS_FILE");
        assertThat(new PushProperties(null, null).provider()).isEqualTo(PushProperties.Provider.NONE);
    }
}
