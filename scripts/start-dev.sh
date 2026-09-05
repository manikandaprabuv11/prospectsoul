#!/usr/bin/env bash
#
# Starts the local ProspectSoul development infrastructure (PostgreSQL,
# Keycloak, MinIO) and reports how to run the backend and frontend.
#
# This script only starts containers. It never installs, upgrades, or removes
# anything on your machine.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

missing=0

# require <command> <install hint> <version command...>
# The version command may print notices (e.g. _JAVA_OPTIONS) before the real
# line, so pick the first line that actually contains a version number.
require() {
  local name="$1" hint="$2"
  if ! command -v "$name" >/dev/null 2>&1; then
    echo "  MISSING  $name — $hint"
    missing=1
    return
  fi
  local version
  version="$("${@:3}" 2>&1 | grep -m 1 -E '[0-9]+\.[0-9]+' || true)"
  echo "  ok       $name (${version:-version unknown})"
}

echo "Checking prerequisites"
require java "install a JDK 21 (e.g. 'sdk install java 21-tem')" java -version
require mvn "install Maven 3.9+ (https://maven.apache.org/download.cgi)" mvn -v
require node "install Node.js 20 LTS or newer (see frontend/.nvmrc)" node -v
require npm "npm ships with Node.js" npm -v
require docker "install Docker (https://docs.docker.com/get-docker/)" docker --version

if command -v docker >/dev/null 2>&1; then
  if docker compose version >/dev/null 2>&1; then
    echo "  ok       docker compose ($(docker compose version --short))"
  else
    echo "  MISSING  docker compose — install the Compose v2 plugin"
    missing=1
  fi
fi

if [ "$missing" -ne 0 ]; then
  echo
  echo "Install the tools listed above, then run this script again." >&2
  exit 1
fi

# The backend needs Java 21; the default 'java' on PATH may be older. Legacy
# JDKs report "1.8.0_x", modern ones "21.0.7", so normalise before comparing.
java_version="$(java -version 2>&1 | sed -n 's/.*version "\([0-9][0-9.]*\).*/\1/p' | head -n 1)"
java_major="${java_version%%.*}"
if [ "$java_major" = "1" ]; then
  java_major="$(printf '%s' "$java_version" | cut -d. -f2)"
fi
if [ -n "$java_major" ] && [ "$java_major" -lt 21 ] 2>/dev/null; then
  echo
  echo "WARNING: 'java' on PATH is $java_version, but the backend targets Java 21."
  echo "         Point JAVA_HOME at a JDK 21 before running Maven, e.g.:"
  echo "           export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64"
fi

if [ ! -f .env ]; then
  echo
  echo "Creating .env from .env.example (development defaults)."
  cp .env.example .env
fi

if [ ! -f frontend/.env.local ]; then
  echo "Creating frontend/.env.local from frontend/.env.example."
  cp frontend/.env.example frontend/.env.local
fi

echo
echo "Starting infrastructure"
# Wait only on the long-running services: --wait treats the one-shot
# minio-init container's normal exit as a failure.
docker compose up -d --wait postgres keycloak minio || {
  echo
  echo "One or more services did not become healthy. Inspect them with:" >&2
  echo "  docker compose ps" >&2
  echo "  docker compose logs -f" >&2
  exit 1
}

# Creates the bucket, then exits. Safe to re-run.
docker compose up -d minio-init >/dev/null

echo
docker compose ps

# Report the ports actually in effect rather than the documented defaults.
set -a
# shellcheck disable=SC1091
. ./.env
set +a

cat <<EOF

Infrastructure is up.

  PostgreSQL      localhost:${POSTGRES_PORT:-5432}   (db ${POSTGRES_DB:-prospectsoul}, user ${POSTGRES_USER:-prospectsoul})
  Keycloak        http://localhost:${KEYCLOAK_PORT:-8081}      admin console: ${KEYCLOAK_ADMIN:-admin}
  Keycloak realm  http://localhost:${KEYCLOAK_PORT:-8081}/realms/vyoog
  MinIO API       http://localhost:${MINIO_PORT:-9000}
  MinIO console   http://localhost:${MINIO_CONSOLE_PORT:-9001}      user ${MINIO_ACCESS_KEY:-prospectsoul}
  MinIO bucket    ${MINIO_BUCKET:-prospectsoul}

Next steps, each in its own terminal:

  Backend    cd backend && mvn spring-boot:run
             -> http://localhost:${SERVER_PORT:-8080}
  Frontend   cd frontend && npm run dev
             -> http://localhost:5173

Keycloak must be running before the backend starts: the resource server
resolves its issuer at start-up.

Stop the infrastructure with scripts/stop-dev.sh (volumes are preserved).
EOF
