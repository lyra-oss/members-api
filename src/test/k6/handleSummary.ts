// Shared by every scenario script: writes the run's metrics as JSON (K6PerformanceSupport copies this file out of
// the container afterwards) instead of leaving the pass/fail numbers to be parsed back out of k6's own console
// text - the same reasoning that led away from Gatling's text-only assertion output. Defining handleSummary()
// suppresses k6's automatic console summary, so this also re-renders it via jslib's own formatter, keeping the
// container logs just as readable as an unmodified run's for local debugging.
// @ts-expect-error -- k6 resolves this over HTTPS at run time; tsc has no way to fetch or type it.
import { textSummary } from "https://jslib.k6.io/k6-summary/0.1.0/index.js";

export function handleSummary(data: object): Record<string, string> {
  return {
    // The trailing sentinel line is what K6PerformanceSupport (Java) watches for on the container's log output to
    // know the run has finished and it's safe to copy summary.json back out - printed last, right before the k6
    // process exits, since a run-to-completion container gives no other reliable "done" signal to wait on.
    stdout: `${textSummary(data, { indent: " ", enableColors: false })}\nK6_RUN_COMPLETE\n`,
    "summary.json": JSON.stringify(data),
  };
}
