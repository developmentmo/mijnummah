package nl.mijnummah.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import nl.mijnummah.AbstractIntegrationTest;
import nl.mijnummah.ApiTestClient.ApiResponse;

/**
 * Sweeps every route the application exposes, so a new endpoint cannot silently be public or reachable for users
 * without a role. Per-object rules (tenant isolation, roles) are covered by the flow tests.
 */
class EndpointSecurityIT extends AbstractIntegrationTest {

    private static final Set<String> PUBLIC_POSTS = Set.of("/api/v1/auth/login", "/api/v1/auth/refresh",
            "/api/v1/auth/logout", "/api/v1/auth/register", "/api/v1/auth/password-reset",
            "/api/v1/auth/password-reset/confirm");

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void everyNonPublicRouteRequiresSigningIn() {
        List<Route> routes = routes();
        assertThat(routes).hasSizeGreaterThan(50);
        UUID mosqueId = mosque("sweep");
        for (Route route : routes) {
            if (route.isPublic()) {
                continue;
            }
            ApiResponse response = route.call(api, mosqueId, null);
            assertThat(response.status()).as("%s anonymous", route).isEqualTo(401);
        }
    }

    @Test
    void aUserWithoutARoleCannotUseAnyAdminRoute() {
        UUID mosqueId = mosque("sweep");
        String token = user("gewone-gebruiker").token();
        for (Route route : routes()) {
            if (!route.path().startsWith("/api/v1/admin/")) {
                continue;
            }
            ApiResponse response = route.call(api, mosqueId, token);
            if (route.method() == RequestMethod.GET && route.path().equals("/api/v1/admin/mosques")) {
                // "The mosques I administer": allowed, and empty for a user without a role.
                assertThat(response.status()).isEqualTo(200);
                assertThat(response.body().size()).isZero();
            } else if (route.hasInput()) {
                // Missing input may be rejected (400) before the role check; it must never succeed.
                assertThat(response.status()).as("%s as plain user", route).isIn(400, 403, 404);
            } else {
                assertThat(response.status()).as("%s as plain user", route).isIn(403, 404);
            }
        }
    }

    @Test
    void apiResponsesCarrySecurityHeaders() {
        for (String path : List.of("/api/v1/public/mosques", "/api/v1/me", "/actuator/health")) {
            ApiResponse response = api.get(path);
            assertThat(response.headers().firstValue("Content-Security-Policy")).as(path)
                    .hasValue("default-src 'none'; frame-ancestors 'none'");
            assertThat(response.headers().firstValue("Referrer-Policy")).as(path).hasValue("no-referrer");
            assertThat(response.headers().firstValue("X-Content-Type-Options")).as(path).hasValue("nosniff");
            assertThat(response.headers().firstValue("X-Frame-Options")).as(path).hasValue("DENY");
        }
    }

    private List<Route> routes() {
        List<Route> routes = new ArrayList<>();
        handlerMapping.getHandlerMethods().forEach((info, handler) -> {
            if (info.getPathPatternsCondition() == null) {
                return;
            }
            boolean hasBody = Arrays.stream(handler.getMethodParameters())
                    .anyMatch(parameter -> parameter.hasParameterAnnotation(RequestBody.class)
                            || isRequired(parameter.getParameterAnnotation(RequestParam.class)));
            for (String path : info.getPathPatternsCondition().getPatternValues()) {
                if (!path.startsWith("/api/")) {
                    continue;
                }
                for (RequestMethod method : info.getMethodsCondition().getMethods()) {
                    routes.add(new Route(method, path, hasBody, handler));
                }
            }
        });
        return routes;
    }

    private static boolean isRequired(RequestParam param) {
        return param != null && param.required() && ValueConstants.DEFAULT_NONE.equals(param.defaultValue());
    }

    /** {@code hasInput}: a request body or required query parameter that the sweep leaves empty. */
    private record Route(RequestMethod method, String path, boolean hasInput, HandlerMethod handler) {

        boolean isPublic() {
            return (method == RequestMethod.GET && path.startsWith("/api/v1/public/"))
                    || (method == RequestMethod.POST && PUBLIC_POSTS.contains(path));
        }

        ApiResponse call(nl.mijnummah.ApiTestClient api, UUID mosqueId, String token) {
            String concrete = path.replace("{mosqueId}", mosqueId.toString())
                    .replace("{slug}", "sweep")
                    .replaceAll("\\{[^}]+}", UUID.randomUUID().toString());
            return switch (method) {
                case GET -> api.get(concrete, token);
                case DELETE -> api.delete(concrete, token);
                case PUT -> api.put(concrete, java.util.Map.of(), token);
                case POST -> api.post(concrete, java.util.Map.of(), token);
                default -> throw new IllegalStateException("Unexpected method in " + this);
            };
        }

        @Override
        public String toString() {
            return method + " " + path;
        }
    }
}
