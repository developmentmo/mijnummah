package nl.mijnummah;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Real PostgreSQL for integration tests, same major version as docker-compose.yml. Skipped when
 * {@code TEST_DATABASE_URL} points at an existing database (machines without Docker); see application-test.yml.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:17-alpine");

    @Bean
    @ServiceConnection
    @ConditionalOnExpression("'${TEST_DATABASE_URL:}'.isEmpty()")
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(POSTGRES_IMAGE);
    }
}
