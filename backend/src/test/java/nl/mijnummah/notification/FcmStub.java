package nl.mijnummah.notification;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * A local stand-in for Google's OAuth token endpoint and the FCM HTTP v1 API, so the real {@code FcmPushGateway}
 * (signing, token exchange, requests, error handling) is tested without a Firebase project. Tokens starting with
 * {@code dead} are answered like uninstalled apps (404 UNREGISTERED).
 */
final class FcmStub {

    static final String PROJECT = "test-project";
    static final String ACCESS_TOKEN = "stub-access-token";

    final List<JsonNode> messages = new CopyOnWriteArrayList<>();
    final List<String> authorizations = new CopyOnWriteArrayList<>();
    final List<SignedJWT> assertions = new CopyOnWriteArrayList<>();

    private final HttpServer server;
    private final KeyPair keys;
    private final JsonMapper json = JsonMapper.builder().build();
    private final Path credentialsFile;

    FcmStub() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            keys = generator.generateKeyPair();
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/token", this::token);
            server.createContext("/v1/projects/" + PROJECT + "/messages:send", this::send);
            server.start();
            credentialsFile = Files.createTempFile("fcm-service-account", ".json");
            Files.writeString(credentialsFile, json.writeValueAsString(Map.of(
                    "type", "service_account",
                    "client_email", "push@test-project.iam.gserviceaccount.com",
                    "private_key", pem(),
                    "token_uri", baseUrl() + "/token")));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    String credentialsFile() {
        return credentialsFile.toString();
    }

    /** Forgets messages; assertions are kept, because the gateway caches its access token across tests. */
    void reset() {
        messages.clear();
        authorizations.clear();
    }

    private void token(HttpExchange exchange) throws IOException {
        String form = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> fields = new java.util.HashMap<>();
        for (String pair : form.split("&")) {
            String[] parts = pair.split("=", 2);
            fields.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
        }
        try {
            SignedJWT assertion = SignedJWT.parse(fields.get("assertion"));
            if (!"urn:ietf:params:oauth:grant-type:jwt-bearer".equals(fields.get("grant_type"))
                    || !assertion.verify(new RSASSAVerifier((RSAPublicKey) keys.getPublic()))) {
                respond(exchange, 400, "{\"error\":\"invalid_grant\"}");
                return;
            }
            assertions.add(assertion);
        } catch (Exception ex) {
            respond(exchange, 400, "{\"error\":\"invalid_request\"}");
            return;
        }
        respond(exchange, 200, "{\"access_token\":\"" + ACCESS_TOKEN + "\",\"expires_in\":3600}");
    }

    private void send(HttpExchange exchange) throws IOException {
        authorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
        JsonNode body = json.readTree(exchange.getRequestBody().readAllBytes());
        String token = body.path("message").path("token").asString();
        if (token.startsWith("dead")) {
            respond(exchange, 404, """
                    {"error":{"code":404,"message":"Requested entity was not found.","status":"NOT_FOUND",
                    "details":[{"@type":"type.googleapis.com/google.firebase.fcm.v1.FcmError","errorCode":"UNREGISTERED"}]}}""");
            return;
        }
        messages.add(body.path("message"));
        respond(exchange, 200, "{\"name\":\"projects/" + PROJECT + "/messages/" + messages.size() + "\"}");
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private String pem() {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(keys.getPrivate().getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + base64 + "\n-----END PRIVATE KEY-----\n";
    }
}
