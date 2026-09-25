package edu.lyra.members.api.performance;

import java.time.Duration;

import io.gatling.javaapi.core.Simulation;

import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.rampUsers;

/**
 * A ramp well past the load {@link LoadSimulation} expects the deliverable to comfortably serve, looking for the
 * point - and the failure mode - where it gives way, not for a clean pass. Part of the nightly performance suite
 * (see {@code nightly-performance.yml}, currently disabled).
 *
 * @author Esteban Cristóbal Rodríguez
 */
public class StressSimulation
        extends Simulation {

    {
        //@formatter:off
        setUp(PerformanceSupport.readScenario().injectOpen(rampUsers(300).during(Duration.ofMinutes(2))))
                .protocols(PerformanceSupport.httpProtocol())
                .assertions(global().failedRequests().percent().lte(5.0));
        //@formatter:on
    }

}
