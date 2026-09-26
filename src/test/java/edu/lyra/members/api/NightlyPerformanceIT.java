package edu.lyra.members.api;

import java.time.Duration;
import java.util.Map;

import edu.lyra.members.api.performance.K6PerformanceSupport;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The heavier performance profiles - {@code load.ts}, {@code stress.ts}, {@code soak.ts} - run only when explicitly
 * requested with {@code -Dperf.nightly=true} (see {@code nightly-performance.yml}, currently disabled), never as
 * part of an ordinary {@code verify}: unlike {@link PerformanceSmokeIT}, a few seconds of extra load, these take
 * minutes and are meant to run on a schedule, not gate every push, pull request or main build.
 *
 * @author Esteban Cristóbal Rodríguez
 */
class NightlyPerformanceIT {

    @Test
    void loadProfile() {
        this.run("load", Duration.ofSeconds(90));
    }

    @Test
    void stressProfile() {
        this.run("stress", Duration.ofMinutes(4));
    }

    @Test
    void soakProfile() {
        this.run("soak", Duration.ofMinutes(23));
    }

    private void run(final String scenario, final Duration waitTimeout) {
        Assumptions.assumeTrue(Boolean.getBoolean("perf.nightly"),
                "Only runs when explicitly requested with -Dperf.nightly=true");
        final boolean passed = K6PerformanceSupport.run(scenario, Map.of(), waitTimeout);
        assertTrue(passed, scenario + ".ts reported threshold failures - see target/k6/" + scenario +
                "-summary.json");
    }

}
