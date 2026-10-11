package nl.mijnummah.identity.infrastructure;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import nl.mijnummah.common.ratelimit.FixedWindowRateLimiter;
import nl.mijnummah.identity.application.LoginThrottle;

/**
 * HTTP security: stateless JWT bearer authentication, deny-by-default.
 * <p>
 * This layer only decides "authenticated or not" plus which endpoints are public. Authorization per mosque
 * ("may this user edit events of this mosque?") is done explicitly in application services through the mosque
 * module's {@code MosqueAccessService}, never by URL patterns.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({CorsProperties.class, RateLimitProperties.class})
class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            UserRepository userRepository,
                                            SecurityErrorHandler securityErrorHandler,
                                            RateLimitProperties rateLimits,
                                            Clock clock) throws Exception {
        var authenticatedUserConverter = new AuthenticatedUserConverter(userRepository);
        http
                // No cookies/sessions are used for authentication, so CSRF protection does not apply.
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout").permitAll()
                        // Published content of active mosques. A valid bearer token is still read, so signed-in
                        // users also see content with visibility REGISTERED_USERS.
                        .requestMatchers(HttpMethod.GET, "/api/v1/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**", "/actuator/info")
                        .permitAll()
                        // API documentation; these paths only exist when mijnummah.api-docs.enabled=true.
                        .requestMatchers(HttpMethod.GET, "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html",
                                "/swagger-ui/**").permitAll()
                        // Boot's error dispatch must not turn real errors into 401s.
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(authenticatedUserConverter))
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler));
        if (rateLimits.enabled()) {
            http.addFilterBefore(new RateLimitFilter(rateLimits, clock, securityErrorHandler),
                    BearerTokenAuthenticationFilter.class);
        }
        return http.build();
    }

    /**
     * CORS for browser clients (the admin portal). Bearer tokens travel in the Authorization header (no cookies),
     * so credentials are not allowed. Mobile apps are not affected by CORS.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        var configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(Duration.ofHours(1));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    LoginThrottle loginThrottle(RateLimitProperties rateLimits, Clock clock) {
        var limit = rateLimits.loginFailuresPerAccount();
        return new LoginThrottle(new FixedWindowRateLimiter(limit.requests(), limit.per(), clock));
    }

    /**
     * Delegating encoder: new hashes use the current default (BCrypt), stored hashes carry their algorithm prefix
     * (e.g. {bcrypt}) so the algorithm can be upgraded (e.g. to Argon2) without invalidating existing passwords.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
