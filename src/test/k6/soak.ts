// A moderate, constant rate of traffic sustained far longer than load.ts's brief burst - long enough for a
// connection leak, an unbounded cache, or a slow memory leak to show up as degrading response times rather than an
// immediate failure. Part of the nightly performance suite (see nightly-performance.yml, currently disabled).
import { readScenario } from "./support.ts";

export const options = {
  scenarios: {
    soak: {
      executor: "constant-arrival-rate",
      rate: 5,
      timeUnit: "1s",
      duration: "20m",
      preAllocatedVUs: 20,
      maxVUs: 100,
    },
  },
  summaryTrendStats: ["avg", "min", "med", "max", "p(90)", "p(95)", "p(99)"],
  thresholds: {
    http_req_failed: ["rate<=0.01"],
    http_req_duration: ["p(99)<3000"],
  },
};

export default function (): void {
  readScenario();
}

export { handleSummary } from "./handleSummary.ts";
