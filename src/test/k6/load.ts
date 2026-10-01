// A ramp to a moderate, expected level of concurrent traffic, sustained briefly - the steady-state load the
// deliverable should comfortably serve. Part of the nightly performance suite (see nightly-performance.yml,
// currently disabled).
import { readScenario } from "./support.ts";

export const options = {
  scenarios: {
    load: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [{ duration: "30s", target: 30 }],
    },
  },
  summaryTrendStats: ["avg", "min", "med", "max", "p(90)", "p(95)"],
  thresholds: {
    http_req_failed: ["rate<=0.01"],
    http_req_duration: ["p(95)<2000"],
  },
};

export default function (): void {
  readScenario();
}

export { handleSummary } from "./handleSummary.ts";
