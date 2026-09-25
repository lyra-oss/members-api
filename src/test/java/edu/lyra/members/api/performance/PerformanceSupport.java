package edu.lyra.members.api.performance;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

/**
 * What every performance simulation in this package shares: where the deliverable under test and its Keycloak
 * realm are, how a virtual user logs in, and the read-only scenario each simulation drives at its own load
 * profile. Business logic and per-endpoint correctness are already covered elsewhere ({@code EndpointSmokeIT}, the
 * unit suite); these simulations only measure how the running deliverable behaves under concurrent load.
 *
 * <p>{@code perf.baseUrl} and {@code perf.tokenUrl} have no built-in default and must be supplied as system
 * properties: the API's version segment and Keycloak's port are both assigned dynamically (a random Testcontainers
 * host port locally, whatever an environment actually runs in CI), so a hardcoded guess here would silently drift
 * out of date. {@code PerformanceSmokeIT} and {@code NightlyPerformanceIT} - the way these simulations are meant to
 * be run - set both automatically from {@code IntegrationTestEnvironment}'s already-running containers; a bare
 * {@code ./mvnw gatling:test} invocation without going through one of those fails fast with a clear message instead
 * of quietly hitting the wrong place.
 *
 * @author Esteban Cristóbal Rodríguez
 */
final class PerformanceSupport {

    static final String BASE_URL  = requireProperty("perf.baseUrl");
    static final String TOKEN_URL = requireProperty("perf.tokenUrl");
    static final String CLIENT_ID = System.getProperty("perf.clientId", "members-api-test");
    static final String USERNAME  = System.getProperty("perf.username", "smoke.parent@example.com");
    static final String PASSWORD  = System.getProperty("perf.password", "password");
    static final String SCOPES    = System.getProperty("perf.scopes",
            "schools.read parents.read kids.read teachers.read classrooms.read");

    private PerformanceSupport() {
    }

    private static String requireProperty(final String name) {
        final String value = System.getProperty(name);
        if(value == null || value.isBlank()) {
            //@formatter:off
            throw new IllegalStateException(
                    ("Missing required system property '%s'. Run this simulation through PerformanceSmokeIT or " +
                     "NightlyPerformanceIT (./mvnw -Dit.test=PerformanceSmokeIT verify), which supply it " +
                     "automatically; only pass it yourself when pointing Gatling at an already-running instance.")
                            .formatted(name));
            //@formatter:on
        }
        return value;
    }

    static HttpProtocolBuilder httpProtocol() {
        return http.baseUrl(BASE_URL).acceptHeader("application/json");
    }

    static ScenarioBuilder readScenario() {
        //@formatter:off
        return scenario("Read the API")
                .exec(authenticate())
                .exec(http("Root").get("/").header("Authorization", "Bearer #{accessToken}").check(status().is(200)))
                .exec(http("List schools").get("/schools").header("Authorization", "Bearer #{accessToken}")
                        .check(status().is(200)))
                .exec(http("List parents").get("/parents").header("Authorization", "Bearer #{accessToken}")
                        .check(status().is(200)))
                .exec(http("List kids").get("/kids").header("Authorization", "Bearer #{accessToken}")
                        .check(status().is(200)))
                .exec(http("List teachers").get("/teachers").header("Authorization", "Bearer #{accessToken}")
                        .check(status().is(200)))
                .exec(http("List classrooms").get("/classrooms").header("Authorization", "Bearer #{accessToken}")
                        .check(status().is(200)));
        //@formatter:on
    }

    private static ChainBuilder authenticate() {
        //@formatter:off
        return exec(http("Authenticate")
                .post(TOKEN_URL)
                .formParam("grant_type", "password")
                .formParam("client_id", CLIENT_ID)
                .formParam("username", USERNAME)
                .formParam("password", PASSWORD)
                .formParam("scope", SCOPES)
                .check(jsonPath("$.access_token").saveAs("accessToken")));
        //@formatter:on
    }

}
