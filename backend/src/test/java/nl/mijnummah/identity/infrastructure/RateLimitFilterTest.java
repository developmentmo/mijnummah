package nl.mijnummah.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import tools.jackson.databind.json.JsonMapper;

class RateLimitFilterTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-11T10:00:00Z"), ZoneOffset.UTC);

    private final RateLimitProperties properties =
            new RateLimitProperties(true, null, null, null, null, null, null, null, null);
    private final RateLimitFilter filter = new RateLimitFilter(
            properties, CLOCK, new SecurityErrorHandler(JsonMapper.builder().build(), CLOCK));

    @Test
    void limitsPasswordChecksPerIpAcrossChangingThePasswordAndDeletingTheAccount() throws Exception {
        int limit = properties.passwordCheck().requests();
        for (int i = 0; i < limit; i++) {
            String method = i % 2 == 0 ? "PUT" : "POST";
            String path = i % 2 == 0 ? "/api/v1/me/password" : "/api/v1/me/deletion";
            assertThat(send(method, path, "10.0.0.1").getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse limited = send("POST", "/api/v1/me/deletion", "10.0.0.1");

        assertThat(limited.getStatus()).isEqualTo(429);
        assertThat(limited.getContentAsString()).contains("RATE_LIMITED");
        assertThat(limited.getHeader("Retry-After")).isNotBlank();
        assertThat(send("PUT", "/api/v1/me/password", "10.0.0.2").getStatus()).isEqualTo(200);
    }

    @Test
    void leavesOtherMethodsAndPathsAlone() throws Exception {
        for (int i = 0; i < 100; i++) {
            assertThat(send("GET", "/api/v1/auth/login", "10.0.0.1").getStatus()).isEqualTo(200);
            assertThat(send("PUT", "/api/v1/me/devices", "10.0.0.1").getStatus()).isEqualTo(200);
        }
    }

    private MockHttpServletResponse send(String method, String path, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
