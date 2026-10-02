#!/bin/bash
#
# Stops the QA environment started by start.sh and drops its data.
#
#   scripts/qa-environment/stop.sh
#   KEEP_DATA=true scripts/qa-environment/stop.sh   # keeps the volumes for the next start
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
COMPOSE_FILE="$REPO_DIR/e2e/src/test/resources/docker-twake-calendar-e2e.yml"

QA_PROJECT="${QA_PROJECT:-twake-qa}"
KEEP_DATA="${KEEP_DATA:-false}"

if [ "$KEEP_DATA" = "true" ]; then
  docker compose -p "$QA_PROJECT" -f "$COMPOSE_FILE" down
else
  docker compose -p "$QA_PROJECT" -f "$COMPOSE_FILE" down --volumes
fi
echo "==> QA environment '$QA_PROJECT' stopped"
