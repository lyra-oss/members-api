package edu.lyra.members.api.performance;

import java.time.Duration;

import io.gatling.javaapi.core.Simulation;

import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.global;

/**
 * A moderate, constant rate of traffic sustained far longer than {@link LoadSimulation}'s brief burst - long enough
 * for a connection leak, an unbounded cache, or a slow memory leak to show up as degrading response times rather
 * than an immediate failure. Part of the nightly performance suite (see {@code nightly-performance.yml}, currently
 * disabled).
 *
 * @author Esteban Cristóbal Rodríguez
 */
public class SoakSimulation
        extends Simulation {

    {
        //@formatter:off
        setUp(PerformanceSupport.readScenario().injectOpen(constantUsersPerSec(5).during(Duration.ofMinutes(20))))
                .protocols(PerformanceSupport.httpProtocol())
                .assertions(
                        global().failedRequests().percent().lte(1.0),
                        global().responseTime().percentile(99).lt(3_000));
        //@formatter:on
    }

}
