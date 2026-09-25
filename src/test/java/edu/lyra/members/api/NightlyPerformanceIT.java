package edu.lyra.members.api;

import edu.lyra.members.api.environment.IntegrationTestEnvironment;
import io.gatling.app.Gatling$;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The heavier performance profiles - {@code LoadSimulation}, {@code StressSimulation}, {@code SoakSimulation} - run
 * only when explicitly requested with {@code -Dperf.nightly=true} (see {@code nightly-performance.yml}, currently
 * disabled), never as part of an ordinary {@code verify}: unlike {@link PerformanceSmokeIT}, a few seconds of extra
 * load, these take minutes and are meant to run on a schedule, not gate every push, pull request or main build.
 *
 * @author Esteban Cristóbal Rodríguez
 */
class NightlyPerformanceIT {

    @Test
    void loadProfile() {
        this.run("edu.lyra.members.api.performance.LoadSimulation");
    }

    @Test
    void stressProfile() {
        this.run("edu.lyra.members.api.performance.StressSimulation");
    }

    @Test
    void soakProfile() {
        this.run("edu.lyra.members.api.performance.SoakSimulation");
    }

    private void run(final String simulationClass) {
        Assumptions.assumeTrue(Boolean.getBoolean("perf.nightly"),
                "Only runs when explicitly requested with -Dperf.nightly=true");
        System.setProperty("perf.baseUrl", IntegrationTestEnvironment.APPLICATION.baseUrl());
        System.setProperty("perf.tokenUrl", IntegrationTestEnvironment.KEYCLOAK.tokenEndpoint());
        //@formatter:off
        final int status =
                Gatling$.MODULE$.fromArgs(new String[] {"-s", simulationClass, "-rf", "target/gatling", "-nr"});
        //@formatter:on
        assertEquals(0, status, simulationClass + " reported failures or assertion violations");
    }

}
