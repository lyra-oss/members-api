package edu.lyra.members.api.environment;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts the PostgreSQL container the application needs at development time, replacing the
 * {@code compose.yml}-based {@code spring-boot-docker-compose} setup. Imported by {@link TestMembersApiApplication}
 * for {@code mvn spring-boot:test-run}.
 *
 * <p>Keycloak is deliberately not started here: unlike PostgreSQL's connection details, which are wired through a
 * bean ({@link ServiceConnection}, resolved by ordinary dependency injection), the JWT issuer URI is an environment
 * property that {@code OAuth2ResourceServerAutoConfiguration} needs already present when its
 * {@code @ConditionalOnProperty}-gated {@code JwtDecoder} bean is evaluated — before any bean in this configuration
 * class runs. {@link TestMembersApiApplication} starts Keycloak itself and passes that property on the command
 * line, which is early enough.
 *
 * @author Esteban Cristóbal Rodríguez
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestEnvironmentConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        //@formatter:off
        return new PostgreSQLContainer(DockerImageName.parse("postgres:18.1"))
                .withDatabaseName("members")
                .withUsername("members")
                .withPassword("members");
        //@formatter:on
    }

}
