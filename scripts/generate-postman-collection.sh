#!/usr/bin/env bash
# Regenerates postman/members-api.postman_collection.json from the application's own OpenAPI description
# (target/openapi.json, written by OpenApiExportTest) - the single source of truth, so the API is never defined
# twice.
#
# Usage:
#   scripts/generate-postman-collection.sh          Regenerate and overwrite the committed collection. Run this
#                                                     after changing the API and commit the result.
#   scripts/generate-postman-collection.sh --check   Regenerate into a temporary file and compare its routes
#                                                     against the committed collection's, without overwriting it.
#                                                     Fails if they differ. Used in CI to catch a forgotten
#                                                     regeneration.
#
# The conversion tool (openapi-to-postmanv2) invents a fresh random id for every item on every run, and - even
# with schema-based (not example-based) generation - still randomizes example values for constrained string
# fields, so two runs of the very same spec are never byte-identical. --check therefore compares each side's
# routes (method and path template only, via jq), not the raw files.
set -euo pipefail
cd "$(dirname "$0")/.."

collection=postman/members-api.postman_collection.json
check=false
[ "${1:-}" = "--check" ] && check=true

routes() {
  jq -r '[.. | objects | select(has("request")) | "\(.request.method) /\(.request.url.path | join("/"))"]
         | sort | unique | .[]' "$1"
}

./mvnw -q -Dcheckstyle.skip=true -Dtest=OpenApiExportTest test

generated=$(mktemp)
trap 'rm -f "$generated" "$generated.clean"' EXIT
npx --yes openapi-to-postmanv2@6.3.3 -s target/openapi.json -o "$generated" \
    -O "schemaFaker=false,parametersResolution=Schema" >/dev/null
jq --sort-keys 'walk(if type == "object" then del(.id, ._postman_id) else . end)' "$generated" > "$generated.clean"

if [ "$check" = true ]; then
  if ! diff -u <(routes "$collection") <(routes "$generated.clean"); then
    echo "$collection's routes don't match the application's current API." >&2
    echo "Run scripts/generate-postman-collection.sh and commit the result." >&2
    exit 1
  fi
  echo "$collection is up to date."
else
  mv "$generated.clean" "$collection"
  echo "Wrote $collection"
fi
