package edu.lyra.members.api;

import java.util.stream.Stream;

import edu.lyra.members.api.environment.KeycloakTestContainer;
import edu.lyra.members.api.environment.TestEnvironmentConfiguration;
import org.springframework.boot.SpringApplication;

/**
 * Entry point for {@code mvn spring-boot:test-run}: delegates to {@link MembersApiApplication#main} on the test
 * classpath, with {@link TestEnvironmentConfiguration} imported so PostgreSQL starts automatically as a
 * {@code @ServiceConnection} bean.
 *
 * <p>Keycloak is started here directly, before {@code SpringApplication} ever runs, and its issuer URI is appended
 * as a command-line argument rather than wired through a bean: {@code OAuth2ResourceServerAutoConfiguration}'s
 * {@code JwtDecoder} bean is {@code @ConditionalOnProperty}-gated, and that condition is evaluated before any bean
 * in {@link TestEnvironmentConfiguration} runs, so the property has to already be part of the environment by the
 * time {@code run} is called — exactly what a command-line argument guarantees.
 *
 * @author Esteban Cristóbal Rodríguez
 */
public class TestMembersApiApplication {

    /**
     * Starts Keycloak, then delegates to {@link MembersApiApplication#main} with its issuer URI appended to
     * {@code args}.
     *
     * @param args the command-line arguments, forwarded on to {@link MembersApiApplication#main}
     */
    public static void main(final String[] args) {
        final KeycloakTestContainer keycloak = new KeycloakTestContainer();
        keycloak.start();
        //@formatter:off
        final String[] argsWithKeycloak = Stream.concat(Stream.of(args), Stream.of(
                "--spring.security.oauth2.resourceserver.jwt.issuer-uri=" + keycloak.issuerUri(),
                "--spring.jpa.properties.jakarta.persistence.schema-generation.database.action=create-drop"))
                .toArray(String[]::new);
        //@formatter:on
        SpringApplication.from(MembersApiApplication::main).with(TestEnvironmentConfiguration.class)
                          .run(argsWithKeycloak);
    }

}
