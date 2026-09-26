package edu.lyra.members.api;

import java.time.Duration;
import java.util.Map;

import edu.lyra.members.api.performance.K6PerformanceSupport;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A quick k6 load-smoke run against the real deliverable, gating the push to the registry the same way the rest of
 * the {@code *IT} suite already does. Skipped when the deliverable under test is the plain-JRE jar image (the
 * fast, every-push feedback path): JVM performance figures don't reflect the native image actually shipped, so
 * they would be noise at best, and running them on every push would blow the push's time budget for no benefit.
 * This runs {@code src/test/k6/smoke.ts}, the same script a developer can invoke directly with
 * {@code k6 run src/test/k6/smoke.ts} against any running instance (given {@code PERF_BASE_URL}/{@code
 * PERF_TOKEN_URL}).
 *
 * @author Esteban Cristóbal Rodríguez
 */
class PerformanceSmokeIT {

    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(90);

    @Test
    void theDeliverableServesConcurrentTrafficCleanly() {
        Assumptions.assumeFalse("jvm".equals(System.getProperty("it.image", "jvm")),
                "Performance figures are only meaningful against the native image; skipped for the JVM jar used " +
                        "for fast per-push feedback");
        final String durationSeconds = System.getProperty("perf.smoke.durationSeconds", "1");
        //@formatter:off
        final boolean passed = K6PerformanceSupport.run("smoke",
                Map.of("PERF_DURATION_SECONDS", durationSeconds), WAIT_TIMEOUT);
        //@formatter:on
        assertTrue(passed, "smoke.ts reported threshold failures - see target/k6/smoke-summary.json");
    }

}
