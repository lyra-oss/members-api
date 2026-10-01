// A handful of virtual users, ramped up over a short (configurable) window - just enough to catch a deliverable
// that is up but broken under any concurrent load at all (a connection pool sized for one, a thread-safety bug
// that only shows up under contention). Run after every native image build on a pull request or on main, gating
// the push to the registry the same way the endpoint IT suite already does.
//
// PERF_DURATION_SECONDS (see pom.xml's perf.smoke.durationSeconds and build-docker.yml's
// performance-test-duration-seconds input) spreads the 3 users' start times over that many seconds rather than
// firing them all at once; it does not change how many requests run - each of the 3 users still executes
// readScenario() exactly once.
import { readScenario } from "./support.ts";

const rampSeconds = Number(__ENV.PERF_DURATION_SECONDS || "1");

export const options = {
  scenarios: {
    smoke: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [{ duration: `${rampSeconds}s`, target: 3 }],
    },
  },
  summaryTrendStats: ["avg", "min", "med", "max", "p(90)", "p(95)"],
  thresholds: {
    http_req_failed: ["rate==0"],
    http_req_duration: ["p(50)<2000", "p(95)<3000", "max<5000"],
  },
};

export default function (): void {
  readScenario();
}

export { handleSummary } from "./handleSummary.ts";
