#!/bin/bash
#
# Builds every docker image the e2e stack needs.
# Run it from the e2e/ directory, before `mvn test`.
#
#   ./pre-build.sh                     # reuses apps/private/dist if present
#   SKIP_FRONTEND_BUILD=true ./pre-build.sh   # never runs npm
#   FORCE_FRONTEND_BUILD=true ./pre-build.sh  # always rebuilds the SPA bundle
#
set -euo pipefail

E2E_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_DIR="$(cd "$E2E_DIR/.." && pwd)"

SKIP_FRONTEND_BUILD="${SKIP_FRONTEND_BUILD:-false}"
FORCE_FRONTEND_BUILD="${FORCE_FRONTEND_BUILD:-false}"

echo "==> Building the private SPA bundle"
if [ "$FORCE_FRONTEND_BUILD" = "true" ] || [ ! -d "$REPO_DIR/apps/private/dist" ]; then
  if [ "$SKIP_FRONTEND_BUILD" = "true" ]; then
    echo "apps/private/dist is missing and SKIP_FRONTEND_BUILD=true. Aborting." >&2
    exit 1
  fi
  NODE_MAJOR="$(node -v 2>/dev/null | sed -E 's/^v([0-9]+).*/\1/')"
  if [ -n "$NODE_MAJOR" ] && [ "$NODE_MAJOR" -ge 24 ]; then
    (cd "$REPO_DIR" && [ -d node_modules ] || npm ci)
    (cd "$REPO_DIR" && npm run build:private)
  else
    # The repo needs Node 24+. Fall back to a container so that a developer running an
    # older Node -- or none at all -- can still run the suite.
    echo "Local Node is ${NODE_MAJOR:-absent}, building the bundle with node:24 in docker"
    docker run --rm \
      --user "$(id -u):$(id -g)" \
      -e HOME=/tmp \
      -e npm_config_cache=/tmp/.npm \
      -v "$REPO_DIR:/app" -w /app \
      node:24 sh -c '[ -d node_modules ] || npm ci; npm run build:private'
  fi
else
  echo "Reusing existing apps/private/dist (FORCE_FRONTEND_BUILD=true to rebuild)"
fi

echo "==> Building the production frontend image"
docker build --quiet -f "$REPO_DIR/apps/private/Dockerfile" \
  --build-arg BUILD_VERSION=e2e \
  -t twake-calendar-web-e2e-base "$REPO_DIR"

echo "==> Layering the e2e runtime configuration on top of it"
docker build --quiet -f "$E2E_DIR/docker/Dockerfile.frontend" -t twake-calendar-web-e2e "$E2E_DIR"

echo "==> Building the backend images"
docker build --quiet --pull -f "$E2E_DIR/docker/Dockerfile.tcalendar" -t tcalendar-e2e "$E2E_DIR"
docker build --quiet --pull -f "$E2E_DIR/docker/Dockerfile.ldap" -t ldap-e2e "$E2E_DIR"
docker build --quiet --pull -f "$E2E_DIR/docker/Dockerfile.dex" -t tcalendar-dex-e2e "$E2E_DIR"
docker build --quiet --pull -f "$E2E_DIR/docker/Dockerfile.proxy" -t tcalendar-proxy-e2e "$E2E_DIR"

# $1: Sabre image to test against, so that esn-sabre CI can point this suite at a candidate build
if [ -n "${1:-}" ]; then
  echo "==> Building the sabre image from $1"
  docker build --quiet --build-arg SABRE_IMAGE="$1" -t sabre-e2e -f "$E2E_DIR/docker/Dockerfile.sabre" "$E2E_DIR"
else
  docker build --quiet --pull -t sabre-e2e -f "$E2E_DIR/docker/Dockerfile.sabre" "$E2E_DIR"
fi

echo "==> All e2e images built"
