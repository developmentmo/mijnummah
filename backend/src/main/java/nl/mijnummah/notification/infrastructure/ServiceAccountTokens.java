package nl.mijnummah.notification.infrastructure;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import nl.mijnummah.notification.application.PushServiceException;

/**
 * OAuth 2.0 access tokens for the FCM API from a Google service account key, with the standard JWT bearer grant
 * (RFC 7523): a JWT signed with the account's private key is exchanged at the account's token endpoint. Tokens
 * are cached until shortly before they expire. Signing uses Nimbus JOSE, which Spring Security already brings in.
 */
class ServiceAccountTokens {

    static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final Duration ASSERTION_LIFETIME = Duration.ofHours(1);
    private static final Duration RENEW_BEFORE_EXPIRY = Duration.ofMinutes(5);

    private final String clientEmail;
    private final RSAPrivateKey privateKey;
    private final URI tokenUri;
    private final RestClient http;
    private final Clock clock;

    private String accessToken;
    private Instant expiresAt = Instant.MIN;

    ServiceAccountTokens(String clientEmail, RSAPrivateKey privateKey, URI tokenUri, RestClient http, Clock clock) {
        this.clientEmail = clientEmail;
        this.privateKey = privateKey;
        this.tokenUri = tokenUri;
        this.http = http;
        this.clock = clock;
    }

    /** Reads a service account key file ({@code client_email}, {@code private_key}, {@code token_uri}). */
    static ServiceAccountTokens fromFile(Path file, JsonMapper json, RestClient http, Clock clock) {
        try {
            JsonNode key = json.readTree(Files.readString(file));
            return new ServiceAccountTokens(required(key, "client_email"), parsePrivateKey(required(key, "private_key")),
                    URI.create(required(key, "token_uri")), http, clock);
        } catch (IOException | RuntimeException ex) {
            throw new IllegalStateException("FCM credentials file is missing or not a service account key: "
                    + ex.getMessage(), ex);
        }
    }

    synchronized String get() {
        Instant now = Instant.now(clock);
        if (accessToken == null || !now.isBefore(expiresAt.minus(RENEW_BEFORE_EXPIRY))) {
            fetch(now);
        }
        return accessToken;
    }

    private void fetch(Instant now) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
        form.add("assertion", assertion(now));
        try {
            JsonNode response = http.post().uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.hasNonNull("access_token")) {
                throw new PushServiceException("Token endpoint returned no access token");
            }
            accessToken = response.get("access_token").asString();
            expiresAt = now.plusSeconds(response.path("expires_in").asLong(3600));
        } catch (RestClientException ex) {
            throw new PushServiceException("Could not obtain an FCM access token: " + ex.getMessage(), ex);
        }
    }

    private String assertion(Instant now) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(clientEmail)
                .subject(clientEmail)
                .audience(tokenUri.toString())
                .claim("scope", SCOPE)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(ASSERTION_LIFETIME)))
                .build();
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
            jwt.sign(new RSASSASigner(privateKey));
            return jwt.serialize();
        } catch (JOSEException ex) {
            throw new PushServiceException("Could not sign the service account assertion", ex);
        }
    }

    static RSAPrivateKey parsePrivateKey(String pem) {
        String base64 = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        try {
            return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalStateException("private_key is not an RSA key in PKCS#8 PEM format", ex);
        }
    }

    private static String required(JsonNode node, String field) {
        if (!node.hasNonNull(field) || node.get(field).asString().isBlank()) {
            throw new IllegalStateException("field '" + field + "' is missing");
        }
        return node.get(field).asString();
    }
}
