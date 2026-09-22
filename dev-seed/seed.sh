#!/usr/bin/env bash
set -euo pipefail

seed_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
exec python3 "$seed_root/scripts/seed.py" "$@"
