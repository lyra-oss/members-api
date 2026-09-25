package edu.lyra.members.api.environment;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * The application under test, run as its own container on the same Docker network as PostgreSQL and Keycloak — the
 * deliverable the integration tests exercise.
 *
 * <p>Two flavours: {@link #jvmJar} packages {@code target/*.jar} (a plain JRE base image plus that one file) for
 * fast, every-push feedback; {@link #preBuiltImage} runs an already-built OCI image — in particular, the native
 * image {@code spring-boot:build-image} just produced — verifying the exact artifact that would be shipped.
 *
 * @author Esteban Cristóbal Rodríguez
 */
public final class ApplicationContainer
        extends GenericContainer<ApplicationContainer> {

    private static final int      APPLICATION_PORT = 8080;
    private static final String   CONTEXT_PATH     = "/v0";
    private static final Duration STARTUP_TIMEOUT  = Duration.ofMinutes(3);

    // Properties needing exact, literal names — e.g. a JPA-native key such as
    // "jakarta.persistence.schema-generation.database.action", whose "schema-generation" segment mixes a hyphen
    // with dots that are themselves significant — can't be set through Spring's environment-variable relaxed
    // binding: OS env var names can't distinguish a hyphen from a dot, so a Map<String, String> property (unlike a
    // typed @ConfigurationProperties field) can't be reconstructed from one unambiguously. Passing them as
    // command-line arguments instead sidesteps that.
    private final List<String> commandPrefix;

    private ApplicationContainer(final DockerImageName image, final List<String> commandPrefix) {
        super(image);
        this.commandPrefix = commandPrefix;
    }

    /**
     * Packages the given jar into a plain JRE base image and runs it.
     *
     * @param jarFile the packaged Spring Boot jar (e.g. {@code target/members-api-0.0.1-SNAPSHOT.jar})
     *
     * @return the container, not yet configured with the rest of the environment or started
     */
    public static ApplicationContainer jvmJar(final Path jarFile) {
        //@formatter:off
        return new ApplicationContainer(DockerImageName.parse("eclipse-temurin:25-jre-noble"), List.of("java", "-jar", "/app.jar"))
                .withCopyFileToContainer(MountableFile.forHostPath(jarFile), "/app.jar");
        //@formatter:on
    }

    /**
     * Runs an already-built image directly — typically the one {@code spring-boot:build-image} just produced.
     *
     * @param imageReference the image reference, as loaded into the local Docker daemon
     *
     * @return the container, not yet configured with the rest of the environment or started
     */
    public static ApplicationContainer preBuiltImage(final String imageReference) {
        // No prefix: the image's own ENTRYPOINT (the buildpacks launcher) runs the application, and the property
        // arguments withEnvironment sets as this container's command are passed to it as plain argv, the same way
        // "docker run <image> --some.property=value" would.
        return new ApplicationContainer(DockerImageName.parse(imageReference), List.of());
    }

    /**
     * Joins {@code network}, and wires PostgreSQL and Keycloak connection details to their network aliases plus the
     * schema-generation and metrics-export overrides the ITs need.
     *
     * @param network             the network PostgreSQL and Keycloak are reachable on
     * @param postgresAlias       PostgreSQL's network alias
     * @param postgresDatabase    the database name
     * @param postgresUsername    the database username
     * @param postgresPassword    the database password
     * @param keycloakIssuerUri   Keycloak's fixed issuer URI (see {@link KeycloakTestContainer#withFixedHostname})
     *
     * @return this container
     */
    public ApplicationContainer withEnvironment(
            final Network network,
            final String postgresAlias,
            final String postgresDatabase,
            final String postgresUsername,
            final String postgresPassword,
            final String keycloakIssuerUri
    ) {
        //@formatter:off
        final Map<String, String> env = Map.of(
                "SPRING_DATASOURCE_URL",
                        "jdbc:postgresql://%s:5432/%s".formatted(postgresAlias, postgresDatabase),
                "SPRING_DATASOURCE_USERNAME", postgresUsername,
                "SPRING_DATASOURCE_PASSWORD", postgresPassword,
                "SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI", keycloakIssuerUri,
                "MANAGEMENT_OTLP_METRICS_EXPORT_ENABLED", "false",
                // Pins the dialect and skips Hibernate's own eager JDBC-metadata connection at boot: this
                // deliverable only ever talks to PostgreSQL, so there is nothing for that connection to
                // auto-detect, and skipping it removes a startup-ordering dependency on PostgreSQL's readiness.
                "SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT", "org.hibernate.dialect.PostgreSQLDialect",
                "SPRING_JPA_PROPERTIES_HIBERNATE_BOOT_ALLOW_JDBC_METADATA_ACCESS", "false");
        final List<String> command = new ArrayList<>(this.commandPrefix);
        command.add("--spring.jpa.properties.jakarta.persistence.schema-generation.database.action=create-drop");
        return this.withNetwork(network)
                   .withExposedPorts(APPLICATION_PORT)
                   .withEnv(env)
                   .withCommand(command.toArray(new String[0]))
                   .withStartupTimeout(STARTUP_TIMEOUT)
                   .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(ApplicationContainer.class)))
                   .waitingFor(Wait.forHttp(CONTEXT_PATH + "/actuator/health").forStatusCode(200)
                                   .withStartupTimeout(STARTUP_TIMEOUT));
        //@formatter:on
    }

    /**
     * The base URL the test JVM (outside Docker) can reach this container's API through.
     *
     * @return the {@code http://<host>:<mapped port><context path>} base URL
     */
    public String baseUrl() {
        return "http://%s:%d%s".formatted(this.getHost(), this.getMappedPort(APPLICATION_PORT), CONTEXT_PATH);
    }

}
