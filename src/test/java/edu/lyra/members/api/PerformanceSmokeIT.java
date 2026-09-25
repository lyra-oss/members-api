package edu.lyra.members.api;

import edu.lyra.members.api.environment.IntegrationTestEnvironment;
import io.gatling.app.Gatling$;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A quick Gatling load-smoke run against the real deliverable, gating the push to the registry the same way the
 * rest of the {@code *IT} suite already does. Skipped when the deliverable under test is the plain-JRE jar image
 * (the fast, every-push feedback path): JVM performance figures don't reflect the native image actually shipped, so
 * they would be noise at best, and running them on every push would blow the push's time budget for no benefit.
 * This runs the same {@code edu.lyra.members.api.performance.SmokeSimulation} a developer can invoke directly with
 * {@code ./mvnw gatling:test -Dgatling.simulationClass=...} against any running instance.
 *
 * @author Esteban Cristóbal Rodríguez
 */
class PerformanceSmokeIT {

    @Test
    void theDeliverableServesConcurrentTrafficCleanly() {
        Assumptions.assumeFalse("jvm".equals(System.getProperty("it.image", "jvm")),
                "Performance figures are only meaningful against the native image; skipped for the JVM jar used " +
                        "for fast per-push feedback");
        System.setProperty("perf.baseUrl", IntegrationTestEnvironment.APPLICATION.baseUrl());
        System.setProperty("perf.tokenUrl", IntegrationTestEnvironment.KEYCLOAK.tokenEndpoint());
        //@formatter:off
        final int status = Gatling$.MODULE$.fromArgs(new String[] {
                "-s", "edu.lyra.members.api.performance.SmokeSimulation",
                "-rf", "target/gatling",
                "-nr"
        });
        //@formatter:on
        assertEquals(0, status, "The performance smoke simulation reported failures or assertion violations");
    }

}
