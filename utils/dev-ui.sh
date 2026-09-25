#!/bin/bash
#
# Run the comic-hub UI dev server locally against the dev API (comics-api-dev on portainer).
#
# Runs on a workstation:
#   - Loads nvm and switches to the Node version in comic-hub/.nvmrc, so it works from
#     non-interactive shells where npm isn't on the PATH
#   - Installs node_modules if they're missing
#   - Checks the dev API answers before starting, so a down backend is obvious up front
#   - Starts `next dev` with NEXT_PUBLIC_GRAPHQL_ENDPOINT pointing at the dev API.
#     An exported variable wins over comic-hub/.env* files, so no .env.local is needed
#
# Usage:
#   ./utils/dev-ui.sh
#   ./utils/dev-ui.sh --api http://localhost:8888/graphql   # e.g. via tunnel-to-prod-api.sh
#   DEV_API_URL=http://localhost:8888/graphql ./utils/dev-ui.sh
#

set -euo pipefail

DEV_API_URL="${DEV_API_URL:-http://portainer.stapledon.ca:8087/graphql}"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
HUB_DIR="$(cd "${SCRIPT_DIR}/../comic-hub" && pwd)"

usage() {
    cat <<EOF
Usage: $0 [--api <graphql-url>]

Starts the comic-hub dev server on http://localhost:3000.

  --api <url>   GraphQL endpoint to use (default: ${DEV_API_URL})
  -h, --help    Show this help

Environment overrides:
  DEV_API_URL   Same as --api
EOF
}

die() {
    echo "Error: $*" >&2
    exit 1
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --api)
            [[ $# -ge 2 ]] || die "--api needs a URL"
            DEV_API_URL="$2"
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            usage
            die "Unknown argument: $1"
            ;;
    esac
done

# --- Node via nvm ---
export NVM_DIR="${NVM_DIR:-$HOME/.nvm}"
[[ -s "$NVM_DIR/nvm.sh" ]] || die "nvm not found at $NVM_DIR"
# shellcheck disable=SC1091
source "$NVM_DIR/nvm.sh"
cd "$HUB_DIR"
nvm use >/dev/null || die "Node version from comic-hub/.nvmrc is not installed (run: nvm install)"

if [[ ! -d node_modules ]]; then
    echo "--- Installing dependencies ---"
    npm ci
fi

# --- Check the API before starting ---
status=$(curl -s -m 5 -o /dev/null -w '%{http_code}' -X POST \
    -H 'Content-Type: application/json' -d '{"query":"{__typename}"}' "$DEV_API_URL" || true)
if [[ "$status" != "200" ]]; then
    echo "Warning: ${DEV_API_URL} answered HTTP ${status:-none}; login and data calls will fail." >&2
fi

echo "--- comic-hub dev server -> ${DEV_API_URL} ---"
export NEXT_PUBLIC_GRAPHQL_ENDPOINT="$DEV_API_URL"
exec npm run dev
