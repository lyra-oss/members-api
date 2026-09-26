package edu.lyra.members.api.performance;

import java.time.Duration;

import io.gatling.javaapi.core.Simulation;

import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.rampUsers;

/**
 * A handful of virtual users, ramped up over a short (configurable) window - just enough to catch a deliverable
 * that is up but broken under any concurrent load at all (a connection pool sized for one, a thread-safety bug
 * that only shows up under contention). Run after every native image build on a pull request or on {@code main},
 * gating the push to the registry the same way the endpoint IT suite already does.
 *
 * <p>{@code perf.smoke.durationSeconds} (see pom.xml's {@code maven-failsafe-plugin} configuration and
 * build-docker.yml's {@code performance-test-duration-seconds} input) spreads the 3 users' start times over that
 * many seconds rather than firing them all at once; it does not change how many requests run - each of the 3
 * users still executes {@link PerformanceSupport#readScenario()} exactly once.
 *
 * @author Esteban Cristóbal Rodríguez
 */
public class SmokeSimulation
        extends Simulation {

    {
        final Duration rampDuration = Duration.ofSeconds(
                Long.parseLong(System.getProperty("perf.smoke.durationSeconds", "1")));
        //@formatter:off
        setUp(PerformanceSupport.readScenario().injectOpen(rampUsers(3).during(rampDuration)))
                .protocols(PerformanceSupport.httpProtocol())
                .assertions(
                        global().failedRequests().count().is(0L),
                        global().responseTime().percentile(50).lt(2_000),
                        global().responseTime().percentile(95).lt(3_000),
                        global().responseTime().max().lt(5_000));
        //@formatter:on
    }

}
