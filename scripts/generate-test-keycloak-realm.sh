#!/usr/bin/env bash
# Regenerates src/test/resources/keycloak/lyra-realm.json, the full realm Testcontainers imports for tests, by
# merging the production-shape realm (keycloak/lyra-realm.json - the security vocabulary: realm roles and client
# scopes) with the test-only fixtures (src/test/resources/keycloak-fixtures/test-fixtures.json - the test client,
# test users, and the relaxed sslRequired a TLS-less test container needs). The security vocabulary is defined
# once, in the production file, so tests and production can never drift apart on what a role or scope means.
#
# The fixtures file deliberately lives outside src/test/resources/keycloak: KeycloakTestContainer mounts that whole
# directory as Keycloak's --import-realm source, which scans every .json file in it as a full realm representation
# - the fixtures fragment (no "realm" key) would fail that import if it sat alongside the generated file.
#
# Usage:
#   scripts/generate-test-keycloak-realm.sh   Regenerate and overwrite the committed test realm. Run this after
#                                              changing roles or client scopes and commit the result.
set -euo pipefail
cd "$(dirname "$0")/.."

production=keycloak/lyra-realm.json
fixtures=src/test/resources/keycloak-fixtures/test-fixtures.json
target=src/test/resources/keycloak/lyra-realm.json

jq --indent 4 -s '.[0] * .[1]' "$production" "$fixtures" > "$target.tmp"
mv "$target.tmp" "$target"
echo "Wrote $target"
