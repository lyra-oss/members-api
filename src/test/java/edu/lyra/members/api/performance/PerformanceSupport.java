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
 * realm are (read from system properties, defaulting to where the Testcontainers-managed IT environment - see
 * {@code IntegrationTestEnvironment} - publishes them locally), how a virtual user logs in, and the read-only
 * scenario each simulation drives at its own load profile. Business logic and per-endpoint correctness are already
 * covered elsewhere ({@code EndpointSmokeIT}, the unit suite); these simulations only measure how the running
 * deliverable behaves under concurrent load.
 *
 * @author Esteban Cristóbal Rodríguez
 */
final class PerformanceSupport {

    static final String BASE_URL = System.getProperty("perf.baseUrl", "http://localhost:8080/v0");
    static final String TOKEN_URL =
            System.getProperty("perf.tokenUrl", "http://localhost:8180/realms/lyra/protocol/openid-connect/token");
    static final String CLIENT_ID = System.getProperty("perf.clientId", "members-api-test");
    static final String USERNAME  = System.getProperty("perf.username", "smoke.parent@example.com");
    static final String PASSWORD  = System.getProperty("perf.password", "password");
    static final String SCOPES    = System.getProperty("perf.scopes",
            "schools.read parents.read kids.read teachers.read classrooms.read");

    private PerformanceSupport() {
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
