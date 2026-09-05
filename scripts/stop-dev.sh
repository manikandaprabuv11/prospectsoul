#!/usr/bin/env bash
#
# Stops the local ProspectSoul development infrastructure.
#
# Named volumes (PostgreSQL data, MinIO objects) are preserved. To discard them
# as well, run explicitly:  docker compose down -v

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if ! docker compose version >/dev/null 2>&1; then
  echo "docker compose is not available on this machine." >&2
  exit 1
fi

docker compose down

echo
echo "Infrastructure stopped. Data volumes were kept."
