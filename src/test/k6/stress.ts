// A ramp well past the load load.ts expects the deliverable to comfortably serve, looking for the point - and the
// failure mode - where it gives way, not for a clean pass. Part of the nightly performance suite (see
// nightly-performance.yml, currently disabled).
import { readScenario } from "./support.ts";

export const options = {
  scenarios: {
    stress: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [{ duration: "2m", target: 300 }],
    },
  },
  summaryTrendStats: ["avg", "min", "med", "max", "p(90)", "p(95)"],
  thresholds: {
    http_req_failed: ["rate<=0.05"],
  },
};

export default function (): void {
  readScenario();
}

export { handleSummary } from "./handleSummary.ts";
