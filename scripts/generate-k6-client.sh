#!/usr/bin/env bash
# Regenerates src/test/k6/generated/lyraMembersAPI.ts from the application's own OpenAPI description
# (target/openapi.json, written by OpenApiExportTest) - the same single source of truth the committed Postman
# collection is generated from, so the API is never defined twice. The k6 performance scripts under src/test/k6
# import this client instead of building HTTP requests by hand.
#
# Usage:
#   scripts/generate-k6-client.sh          Regenerate and overwrite the committed client. Run this after changing
#                                           the API and commit the result.
#   scripts/generate-k6-client.sh --check  Regenerate into a temporary file and compare it against the committed
#                                           client, without overwriting it. Fails if they differ. Used in CI to
#                                           catch a forgotten regeneration.
#
# Unlike the Postman conversion (openapi-to-postmanv2, which invents random ids and faked example values on every
# run), @grafana/openapi-to-k6 produces byte-identical output for the same input, so --check can diff the raw files
# directly instead of needing to normalize them first.
set -euo pipefail
cd "$(dirname "$0")/.."

client=src/test/k6/generated/lyraMembersAPI.ts
check=false
[ "${1:-}" = "--check" ] && check=true

./mvnw -q -Dcheckstyle.skip=true -Dtest=OpenApiExportTest test

workdir=$(mktemp -d)
trap 'rm -rf "$workdir"' EXIT
# The output directory must not already exist - openapi-to-k6 silently generates nothing (exit 0) otherwise - so
# it's a fresh subdirectory of workdir, never workdir itself.
generated="$workdir/out"
npx --yes @grafana/openapi-to-k6@0.4.1 target/openapi.json "$generated" --disable-analytics >/dev/null

# @ts-nocheck: the tool's own output doesn't type-check cleanly against @types/k6 under strict mode (its response
# bodies are typed as the narrow response model, but @types/k6's Response#json() actually returns
# ArrayBuffer | JSONValue) - not something to hand-patch in generated code we don't own. Everything that actually
# imports and calls this client (src/test/k6/support.ts) is still fully type-checked as normal.
{ echo "// @ts-nocheck"; cat "$generated/lyraMembersAPI.ts"; } > "$generated/checked.ts"

if [ "$check" = true ]; then
  if ! diff -u "$client" "$generated/checked.ts"; then
    echo "$client doesn't match the application's current API." >&2
    echo "Run scripts/generate-k6-client.sh and commit the result." >&2
    exit 1
  fi
  echo "$client is up to date."
else
  mv "$generated/checked.ts" "$client"
  echo "Wrote $client"
fi
