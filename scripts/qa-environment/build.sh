#!/bin/bash
#
# Builds the docker images start.sh runs: both SPA bundles, the frontend images layered with the
# e2e runtime configuration, and the backend images. Same images as the e2e suite.
#
#   scripts/qa-environment/build.sh                      # rebuilds the SPA bundles from the working tree
#   FORCE_FRONTEND_BUILD=false scripts/qa-environment/build.sh   # reuses apps/*/dist if present
#   scripts/qa-environment/build.sh <sabre image>        # tests against a candidate esn-sabre build
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

# QA is about the code in the working tree: unlike pre-build.sh, never reuse a stale bundle unless
# asked to.
export FORCE_FRONTEND_BUILD="${FORCE_FRONTEND_BUILD:-true}"

cd "$REPO_DIR/e2e"
./pre-build.sh "$@"
