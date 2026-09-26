package edu.lyra.members.api.environment;

import java.nio.file.Path;

import org.testcontainers.containers.Network;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * The multi-container environment the {@code *IT} suite runs against: PostgreSQL, Keycloak and the application
 * itself, all on one Docker network — replacing {@code spring-boot-maven-plugin}'s {@code start}/{@code stop} goals
 * (a forked JVM process reading {@code compose.yml}-provisioned services by a hardcoded port) with the deliverable
 * actually under test.
 *
 * <p>Started once per JVM, on first use, and left running for every {@code *IT} class in the same Failsafe fork to
 * share — the well-known Testcontainers "singleton container" pattern. Cleanup is left to the Ryuk reaper Testcontainers
 * itself starts, the same way {@link TestEnvironmentConfiguration} and {@link KeycloakTestContainer} already rely on
 * it for {@code spring-boot:test-run}.
 *
 * <p>Which deliverable is exercised is controlled by the {@code it.image} system property Failsafe is configured to
 * pass: {@code "jvm"} (the default) packages {@code target/*.jar}; any other value is treated as an already-built
 * OCI image reference.
 *
 * @author Esteban Cristóbal Rodríguez
 */
public final class IntegrationTestEnvironment {

    private static final String POSTGRES_ALIAS    = "postgres";
    private static final String POSTGRES_DATABASE = "members";
    private static final String POSTGRES_USERNAME = "members";
    private static final String POSTGRES_PASSWORD = "members";
    private static final String KEYCLOAK_ALIAS     = "keycloak";
    // K6PerformanceSupport addresses both this and KEYCLOAK_ALIAS by name, since it runs k6 as its own container
    // on NETWORK rather than in this JVM (as the test JVM itself - and the perf.baseUrl/perf.tokenUrl system
    // properties the old Gatling-based simulations read - used to).
    public static final String APPLICATION_ALIAS = "application";

    public static final Network              NETWORK  = Network.newNetwork();
    public static final PostgreSQLContainer  POSTGRES = startPostgres();
    public static final KeycloakTestContainer KEYCLOAK = startKeycloak();
    public static final ApplicationContainer APPLICATION = startApplication();

    private IntegrationTestEnvironment() {
    }

    private static PostgreSQLContainer startPostgres() {
        //@formatter:off
        final PostgreSQLContainer container = new PostgreSQLContainer(DockerImageName.parse("postgres:18.1"))
                .withDatabaseName(POSTGRES_DATABASE)
                .withUsername(POSTGRES_USERNAME)
                .withPassword(POSTGRES_PASSWORD)
                .withNetwork(NETWORK)
                .withNetworkAliases(POSTGRES_ALIAS);
        //@formatter:on
        container.start();
        return container;
    }

    private static KeycloakTestContainer startKeycloak() {
        final KeycloakTestContainer container = new KeycloakTestContainer().withFixedHostname(NETWORK, KEYCLOAK_ALIAS);
        container.start();
        return container;
    }

    private static ApplicationContainer startApplication() {
        //@formatter:off
        final String image = System.getProperty("it.image", "jvm");
        final ApplicationContainer container = "jvm".equals(image)
                ? ApplicationContainer.jvmJar(Path.of(System.getProperty("it.jarFile")))
                : ApplicationContainer.preBuiltImage(image);
        container.withEnvironment(NETWORK, APPLICATION_ALIAS, POSTGRES_ALIAS, POSTGRES_DATABASE, POSTGRES_USERNAME,
                                  POSTGRES_PASSWORD, KEYCLOAK.issuerUri());
        //@formatter:on
        container.start();
        return container;
    }

}
