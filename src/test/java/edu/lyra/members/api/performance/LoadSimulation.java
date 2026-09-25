package edu.lyra.members.api.performance;

import java.time.Duration;

import io.gatling.javaapi.core.Simulation;

import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.rampUsers;

/**
 * A ramp to a moderate, expected level of concurrent traffic, sustained briefly - the steady-state load the
 * deliverable should comfortably serve. Part of the nightly performance suite (see {@code nightly-performance.yml},
 * currently disabled).
 *
 * @author Esteban Cristóbal Rodríguez
 */
public class LoadSimulation
        extends Simulation {

    {
        //@formatter:off
        setUp(PerformanceSupport.readScenario().injectOpen(rampUsers(30).during(Duration.ofSeconds(30))))
                .protocols(PerformanceSupport.httpProtocol())
                .assertions(
                        global().failedRequests().percent().lte(1.0),
                        global().responseTime().percentile(95).lt(2_000));
        //@formatter:on
    }

}
