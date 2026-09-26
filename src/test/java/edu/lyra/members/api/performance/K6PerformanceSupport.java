package edu.lyra.members.api.performance;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import edu.lyra.members.api.environment.ApplicationContainer;
import edu.lyra.members.api.environment.IntegrationTestEnvironment;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.k6.K6Container;
import org.testcontainers.utility.MountableFile;

/**
 * Runs one of this package's k6 scripts (see {@code src/test/k6}) as its own container on the same Docker network
 * as {@link IntegrationTestEnvironment}'s already-running Keycloak and application containers, the way
 * {@code PerformanceSmokeIT} and {@code NightlyPerformanceIT} are meant to be run.
 *
 * <p>k6 runs in its own container rather than embedded in this JVM (the way Gatling previously was) because k6
 * itself is a compiled Go binary with no JVM embedding story; running it as a container is the officially supported
 * integration path (see {@code org.testcontainers:testcontainers-k6}).
 *
 * @author Esteban Cristóbal Rodríguez
 */
public final class K6PerformanceSupport {

    private static final Path K6_SOURCE_DIR = Path.of("src", "test", "k6");
    private static final Path SUMMARY_OUTPUT_DIR = Path.of("target", "k6");

    // The k6 image's own default working directory - see K6Container.withTestScript, which copies the script
    // there under its own name, and handleSummary.ts, which writes "summary.json" as a path relative to it.
    private static final String CONTAINER_WORKDIR = "/home/k6";

    // Printed by handleSummary.ts as the last thing the k6 process writes to stdout, right before it exits - the
    // signal this run-to-completion container has actually finished, since Testcontainers has no built-in
    // "wait until the container exits" strategy of its own.
    private static final String COMPLETION_MARKER = "K6_RUN_COMPLETE";

    private static final Duration EXIT_POLL_TIMEOUT = Duration.ofSeconds(10);

    private K6PerformanceSupport() {
    }

    /**
     * Runs {@code <scenario>.ts} to completion against {@link IntegrationTestEnvironment}'s application and
     * Keycloak containers, and copies back the JSON summary {@code handleSummary.ts} wrote.
     *
     * @param scenario     the script's base name, e.g. {@code "smoke"} for {@code src/test/k6/smoke.ts}
     * @param scriptVars   extra {@code __ENV} variables the script reads beyond the connection details this method
     *                     already supplies (e.g. {@code PERF_DURATION_SECONDS})
     * @param waitTimeout  how long to wait for the script to finish before giving up
     *
     * @return {@code true} if the k6 process exited successfully (every threshold passed), {@code false} otherwise
     *         - either way, {@code target/k6/<scenario>-summary.json} is written for inspection
     */
    public static boolean run(final String scenario, final Map<String, String> scriptVars,
            final Duration waitTimeout) {
        final String image = System.getProperty("k6.image", "grafana/k6:2.3.0");
        //@formatter:off
        try(K6Container container = new K6Container(image)
                .withNetwork(IntegrationTestEnvironment.NETWORK)
                .withCopyFileToContainer(hostFile("support.ts"), CONTAINER_WORKDIR + "/support.ts")
                .withCopyFileToContainer(hostFile("handleSummary.ts"), CONTAINER_WORKDIR + "/handleSummary.ts")
                .withCopyFileToContainer(hostFile("generated/lyraMembersAPI.ts"),
                        CONTAINER_WORKDIR + "/generated/lyraMembersAPI.ts")
                .withTestScript(hostFile(scenario + ".ts"))
                .withCmdOptions("--quiet", "--no-usage-report")
                .withScriptVar("PERF_BASE_URL", ApplicationContainer.networkUrl(IntegrationTestEnvironment.APPLICATION_ALIAS))
                .withScriptVar("PERF_TOKEN_URL", IntegrationTestEnvironment.KEYCLOAK.issuerUri() + "/protocol/openid-connect/token")) {
            //@formatter:on
            scriptVars.forEach(container::withScriptVar);
            container.start();
            awaitCompletion(container, waitTimeout);
            copySummary(container, scenario);
            return exitedSuccessfully(container.getContainerId());
        }
    }

    private static MountableFile hostFile(final String relativePath) {
        return MountableFile.forHostPath(K6_SOURCE_DIR.resolve(relativePath));
    }

    private static void awaitCompletion(final K6Container container, final Duration waitTimeout) {
        final WaitingConsumer consumer = new WaitingConsumer();
        container.followOutput(consumer);
        try {
            consumer.waitUntil(frame -> frame.getUtf8String().contains(COMPLETION_MARKER),
                    Math.toIntExact(waitTimeout.toSeconds()), TimeUnit.SECONDS);
        } catch(final TimeoutException e) {
            throw new IllegalStateException(
                    "k6 didn't finish " + container.getDockerImageName() + " within " + waitTimeout
                            + " - logs:\n" + container.getLogs(), e);
        }
    }

    private static void copySummary(final K6Container container, final String scenario) {
        try {
            Files.createDirectories(SUMMARY_OUTPUT_DIR);
            container.copyFileFromContainer(CONTAINER_WORKDIR + "/summary.json",
                    SUMMARY_OUTPUT_DIR.resolve(scenario + "-summary.json").toString());
        } catch(final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * k6 exits non-zero when any threshold fails, the same way Gatling's own assertions used to fail the run - see
     * each script's {@code thresholds} option. {@link WaitingConsumer#waitUntil} only guarantees the completion
     * marker has been logged, which can race the container's own state transition to "exited" by a moment, so this
     * polls briefly rather than reading a possibly stale/absent exit code immediately.
     */
    private static boolean exitedSuccessfully(final String containerId) {
        final long deadline = System.nanoTime() + EXIT_POLL_TIMEOUT.toNanos();
        while(true) {
            //@formatter:off
            final Long exitCode = DockerClientFactory.instance().client().inspectContainerCmd(containerId).exec()
                                                      .getState().getExitCodeLong();
            //@formatter:on
            if(exitCode != null) {
                return exitCode == 0;
            }
            if(System.nanoTime() > deadline) {
                throw new IllegalStateException("k6 container " + containerId + " never reported an exit code");
            }
            try {
                Thread.sleep(Duration.ofMillis(200));
            } catch(final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }

}
