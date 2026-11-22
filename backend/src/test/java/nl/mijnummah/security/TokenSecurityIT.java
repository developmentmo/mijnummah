package nl.mijnummah.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.function.Consumer;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import nl.mijnummah.AbstractIntegrationTest;
import nl.mijnummah.identity.infrastructure.SecurityProperties;

/**
 * Access tokens the backend did not issue, or no longer accepts, never get in: another key, no signature, expired,
 * another issuer, an unknown or blocked account. Rights come from the database, never from the token.
 */
class TokenSecurityIT extends AbstractIntegrationTest {

    private static final String ME = "/api/v1/auth/me";

    @Autowired
    private SecurityProperties properties;

    @Test
    void aTokenSignedWithTheRealKeyIsAccepted() {
        Account user = user("controle");
        assertThat(api.get(ME, token(properties.jwt().secret(), claims -> claims.subject(user.id().toString())))
                .status()).as("sanity check of the forging helper").isEqualTo(200);
    }

    @Test
    void aTokenSignedWithAnotherKeyIsRefused() {
        Account user = user("vervalst");
        String forged = token("another-secret-that-is-long-enough-for-hs256-0123456789",
                claims -> claims.subject(user.id().toString()));
        assertThat(api.get(ME, forged).status()).isEqualTo(401);
    }

    @Test
    void anUnsignedTokenIsRefused() {
        Account user = user("ongetekend");
        Base64.Encoder base64 = Base64.getUrlEncoder().withoutPadding();
        String header = base64.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = base64.encodeToString(("{\"sub\":\"" + user.id() + "\",\"iss\":\""
                + properties.jwt().issuer() + "\",\"exp\":" + Instant.now().plusSeconds(600).getEpochSecond() + "}")
                .getBytes(StandardCharsets.UTF_8));
        assertThat(api.get(ME, header + "." + payload + ".").status()).isEqualTo(401);
    }

    @Test
    void anExpiredTokenIsRefused() {
        Account user = user("verlopen");
        String expired = token(properties.jwt().secret(), claims -> claims.subject(user.id().toString())
                .issuedAt(Instant.now().minusSeconds(3600)).expiresAt(Instant.now().minusSeconds(1800)));
        assertThat(api.get(ME, expired).status()).isEqualTo(401);
    }

    @Test
    void aTokenOfAnotherIssuerIsRefused() {
        Account user = user("uitgever");
        String foreign = token(properties.jwt().secret(),
                claims -> claims.subject(user.id().toString()).issuer("iemand-anders"));
        assertThat(api.get(ME, foreign).status()).isEqualTo(401);
    }

    @Test
    void aTokenForAnUnknownOrBlockedAccountIsRefused() {
        assertThat(api.get(ME, token(properties.jwt().secret(), claims -> claims.subject(UUID.randomUUID().toString())))
                .status()).isEqualTo(401);
        assertThat(api.get(ME, token(properties.jwt().secret(), claims -> claims.subject("geen-uuid"))).status())
                .isEqualTo(401);

        Account blocked = user("geblokkeerd");
        api.post("/api/v1/admin/users/" + blocked.id() + "/block", null, platformAdmin.token());
        assertThat(api.get(ME, blocked.token()).status()).isEqualTo(401);
    }

    @Test
    void aRoleClaimInTheTokenGivesNoRights() {
        Account user = user("rolclaim");
        UUID mosqueId = mosque("claim");
        String withClaims = token(properties.jwt().secret(), claims -> claims.subject(user.id().toString())
                .claim("platformRole", "PLATFORM_ADMIN").claim("roles", java.util.List.of("PLATFORM_ADMIN"))
                .claim("scope", "admin"));
        assertThat(api.get("/api/v1/admin/platform/dashboard", withClaims).status()).isEqualTo(403);
        assertThat(api.get("/api/v1/admin/mosques/" + mosqueId, withClaims).status()).isEqualTo(403);
        assertThat(api.get("/api/v1/admin/users", withClaims).status()).isEqualTo(403);
    }

    /** Signs a token like the backend does (HS256), with the issuer and a short lifetime unless changed. */
    private String token(String secret, Consumer<JwtClaimsSet.Builder> customize) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(600))
                .id(UUID.randomUUID().toString());
        customize.accept(claims);
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build()))
                .getTokenValue();
    }
}
