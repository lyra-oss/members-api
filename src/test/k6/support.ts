// What every performance script in this directory shares: where the deliverable under test and its Keycloak realm
// are, how a virtual user logs in, and the read-only scenario each script drives at its own load profile. Business
// logic and per-endpoint correctness are already covered elsewhere (EndpointSmokeIT, the unit suite); this only
// measures how the running deliverable behaves under concurrent load.
//
// PERF_BASE_URL and PERF_TOKEN_URL have no built-in default and must be supplied as environment variables: the
// API's version segment and Keycloak's port are both assigned dynamically (a random Testcontainers host port
// locally, whatever CI actually runs in), so a hardcoded guess here would silently drift out of date.
// K6PerformanceSupport - the way these scripts are meant to be run - sets both automatically from
// IntegrationTestEnvironment's already-running containers; a bare "k6 run" invocation without going through it
// fails fast with a clear message instead of quietly hitting the wrong place.
import http from "k6/http";
import { check } from "k6";
import type { Params, Response } from "k6/http";

import { LyraMembersAPIClient } from "./generated/lyraMembersAPI.ts";

function requireEnv(name: string): string {
  const value = __ENV[name];
  if (!value) {
    throw new Error(
      `Missing required environment variable '${name}'. Run this script through K6PerformanceSupport ` +
        "(PerformanceSmokeIT/NightlyPerformanceIT), which supplies it automatically; only pass it yourself when " +
        "pointing k6 at an already-running instance.",
    );
  }
  return value;
}

const BASE_URL = requireEnv("PERF_BASE_URL");
const TOKEN_URL = requireEnv("PERF_TOKEN_URL");
const CLIENT_ID = __ENV.PERF_CLIENT_ID || "members-api-test";
const USERNAME = __ENV.PERF_USERNAME || "smoke.parent@example.com";
const PASSWORD = __ENV.PERF_PASSWORD || "password";
const SCOPES =
  __ENV.PERF_SCOPES || "schools.read parents.read kids.read teachers.read classrooms.read";

const client = new LyraMembersAPIClient({ baseUrl: BASE_URL });

function authenticate(): string {
  const response = http.post(TOKEN_URL, {
    grant_type: "password",
    client_id: CLIENT_ID,
    username: USERNAME,
    password: PASSWORD,
    scope: SCOPES,
  });
  check(response, { "authenticate: 200": (r) => r.status === 200 });
  return response.json("access_token") as string;
}

function expect200(name: string, response: Response): void {
  check(response, { [`${name}: 200`]: (r) => r.status === 200 });
}

/**
 * One virtual user's pass through the read-only scenario: authenticate, then list every domain resource - the same
 * seven requests (one login, six reads) every performance script in this directory drives at its own load profile.
 */
export function readScenario(): void {
  const auth: Params = { headers: { Authorization: `Bearer ${authenticate()}` } };

  expect200("root", client.rootIndex(auth).response);
  expect200("schools", client.schoolFindAll({ pageable: {} }, auth).response);
  expect200("parents", client.parentFindAll({ pageable: {} }, auth).response);
  expect200("kids", client.kidFindAll({ pageable: {} }, auth).response);
  expect200("teachers", client.teacherFindAll({ pageable: {} }, auth).response);
  expect200("classrooms", client.classroomFindAll({ pageable: {} }, auth).response);
}
