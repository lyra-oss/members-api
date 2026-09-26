package edu.lyra.members.api.environment;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * A Keycloak container pre-configured with the test realm ({@code src/test/resources/keycloak}).
 *
 * <p>By default it is left in its dynamic hostname mode: it stamps the {@code iss} claim of every token with
 * whatever host and port the caller used to reach it, so {@link #issuerUri()} — computed from the same randomly
 * assigned host port every caller (in-JVM application code and test code alike) addresses it through — is always
 * the value tokens actually carry. This is what {@link KeycloakTestContainer#KeycloakTestContainer()} gives you, for
 * callers that are not themselves inside the Docker network Keycloak runs on.
 *
 * <p>{@link #withFixedHostname(Network, String)} switches to an explicit, fixed issuer instead: needed whenever a
 * caller that lives on the same Docker network (a container under test, addressing Keycloak by its network alias)
 * and a caller that does not (the test JVM, addressing it by its randomly assigned host port) must agree on one
 * issuer despite reaching Keycloak two different ways.
 *
 * @author Esteban Cristóbal Rodríguez
 */
public final class KeycloakTestContainer
        extends GenericContainer<KeycloakTestContainer> {

    private static final String IMAGE      = "keycloak/keycloak:26.5";
    private static final int    PORT       = 8080;
    private static final String REALM_PATH = "/realms/lyra";

    private String fixedIssuerUri;

    public KeycloakTestContainer() {
        //@formatter:off
        super(DockerImageName.parse(IMAGE));
        this.withCommand("start-dev", "--import-realm")
            .withExposedPorts(PORT)
            .withClasspathResourceMapping("keycloak", "/opt/keycloak/data/import", BindMode.READ_ONLY)
            .waitingFor(Wait.forHttp(REALM_PATH).forStatusCode(200));
        //@formatter:on
    }

    /**
     * Fixes the issuer every token carries to {@code http://<alias>:8080/realms/lyra}, regardless of which URL a
     * given caller used to reach this container, and joins {@code network} under that alias.
     *
     * @param network the network to join
     * @param alias   the network alias other containers on {@code network} will reach this one by
     *
     * @return this container
     */
    public KeycloakTestContainer withFixedHostname(final Network network, final String alias) {
        // KC_HOSTNAME is the base URL Keycloak appends "/realms/<realm>" to itself; passing the realm path here
        // too would double it up into ".../realms/lyra/realms/lyra".
        final String baseUrl = "http://%s:%d".formatted(alias, PORT);
        this.fixedIssuerUri = baseUrl + REALM_PATH;
        return this.withNetwork(network).withNetworkAliases(alias).withEnv("KC_HOSTNAME", baseUrl);
    }

    /**
     * The issuer URI every token this realm issues carries, once the container is started.
     *
     * @return the fixed issuer set by {@link #withFixedHostname(Network, String)}, or, absent that, the
     * {@code http://<host>:<mapped port>/realms/lyra} issuer dynamically derived from how this JVM reaches the
     * container
     */
    public String issuerUri() {
        return this.fixedIssuerUri != null ? this.fixedIssuerUri
                : "http://%s:%d%s".formatted(this.getHost(), this.getMappedPort(PORT), REALM_PATH);
    }

    /**
     * The token endpoint a caller outside Docker (the test JVM) can reach this container's realm through, via its
     * randomly assigned host port — distinct from {@link #issuerUri()}, which may instead be a fixed network alias
     * only containers on the same network can resolve.
     *
     * @return the {@code http://<host>:<mapped port>/realms/lyra/protocol/openid-connect/token} URL
     */
    public String tokenEndpoint() {
        return "http://%s:%d%s/protocol/openid-connect/token".formatted(this.getHost(), this.getMappedPort(PORT),
                REALM_PATH);
    }

}
