package edu.lyra.members.api.performance;

import io.gatling.javaapi.core.Simulation;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.global;

/**
 * A handful of virtual users, once - just enough to catch a deliverable that is up but broken under any concurrent
 * load at all (a connection pool sized for one, a thread-safety bug that only shows up under contention). Run after
 * every native image build on a pull request or on {@code main}, gating the push to the registry the same way the
 * endpoint IT suite already does.
 *
 * @author Esteban Cristóbal Rodríguez
 */
public class SmokeSimulation
        extends Simulation {

    {
        setUp(PerformanceSupport.readScenario().injectOpen(atOnceUsers(3))).protocols(PerformanceSupport.httpProtocol())
                .assertions(global().failedRequests().count().is(0L), global().responseTime().max().lt(5_000));
    }

}
