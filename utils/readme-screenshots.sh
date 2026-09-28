#!/bin/bash
#
# Regenerate the README screenshots and banner in docs/images/readme/.
#
# Runs on a workstation. The screenshots must never show real comics, so the UI runs
# against a local demo API that serves invented comics with original placeholder art:
#   - Starts utils/readme-demo/demo-api.mjs (the real GraphQL schema, invented data)
#   - Starts the comic-hub dev server through utils/dev-ui.sh --api, pointed at it
#   - Captures light-mode desktop and phone screenshots with headless Chrome
#     (utils/readme-demo/capture.mjs), then stops both servers
#
# Needs Google Chrome (or set CHROME) and port 3000 free: stop any other comic-hub
# dev server first, since Next allows one per checkout.
#
# Usage:
#   ./utils/readme-screenshots.sh
#   CHROME=chromium ./utils/readme-screenshots.sh
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
DEMO_DIR="${SCRIPT_DIR}/readme-demo"
OUT_DIR="${ROOT_DIR}/docs/images/readme"
DEMO_PORT="${DEMO_API_PORT:-8099}"
UI_URL="http://localhost:3000"
LOG_DIR="$(mktemp -d)"

die() {
    echo "Error: $*" >&2
    exit 1
}

case "${1:-}" in
    -h|--help)
        sed -n '2,19p' "$0" | sed 's/^# \{0,1\}//'
        exit 0
        ;;
    "") ;;
    *) die "Unknown argument: $1" ;;
esac

command -v "${CHROME:-google-chrome}" >/dev/null || die "Chrome not found (set CHROME to its path)"
if curl -s -m 2 -o /dev/null "$UI_URL"; then
    die "Something is already serving ${UI_URL}; stop the other comic-hub dev server first"
fi

# --- Node via nvm (the same version dev-ui.sh uses) ---
export NVM_DIR="${NVM_DIR:-$HOME/.nvm}"
[[ -s "$NVM_DIR/nvm.sh" ]] || die "nvm not found at $NVM_DIR"
# shellcheck disable=SC1091
source "$NVM_DIR/nvm.sh"
nvm use "$(cat "${ROOT_DIR}/comic-hub/.nvmrc")" >/dev/null || die "Node version from comic-hub/.nvmrc is not installed (run: nvm install)"

# The demo API loads graphql from comic-hub, so install before starting it (a fresh
# worktree has no node_modules yet)
if [[ ! -d "${ROOT_DIR}/comic-hub/node_modules" ]]; then
    echo "--- Installing dependencies ---"
    (cd "${ROOT_DIR}/comic-hub" && npm ci)
fi

pids=()
cleanup() {
    for pid in "${pids[@]}"; do
        # dev-ui.sh execs npm, which starts next as a child: stop the whole group
        kill -- -"$pid" 2>/dev/null || kill "$pid" 2>/dev/null || true
    done
    wait 2>/dev/null || true
}
trap cleanup EXIT

wait_for() {
    local url="$1" name="$2" tries="${3:-120}"
    for ((i = 0; i < tries; i++)); do
        curl -s -m 2 -o /dev/null "$url" && return 0
        sleep 1
    done
    die "${name} didn't start; logs in ${LOG_DIR}"
}

echo "--- Demo API on :${DEMO_PORT} ---"
DEMO_API_PORT="$DEMO_PORT" setsid node "${DEMO_DIR}/demo-api.mjs" >"${LOG_DIR}/demo-api.log" 2>&1 &
pids+=($!)
wait_for "http://localhost:${DEMO_PORT}/banner" "Demo API" 20

echo "--- comic-hub dev server -> demo API ---"
setsid "${SCRIPT_DIR}/dev-ui.sh" --api "http://localhost:${DEMO_PORT}/graphql" >"${LOG_DIR}/dev-ui.log" 2>&1 &
pids+=($!)
wait_for "${UI_URL}/login" "comic-hub dev server"

echo "--- Capturing to ${OUT_DIR#"${ROOT_DIR}"/} ---"
node "${DEMO_DIR}/capture.mjs" --ui "$UI_URL" --out "$OUT_DIR" --banner "http://localhost:${DEMO_PORT}/banner"

# The demo logs a line for each query it can't answer and each unknown path
if grep -v 'http://localhost' "${LOG_DIR}/demo-api.log" | grep -q '\[demo-api\]'; then
    echo "Demo API warnings:" >&2
    grep -v 'http://localhost' "${LOG_DIR}/demo-api.log" | grep '\[demo-api\]' | sort | uniq -c >&2
fi

# Shrink the PNGs when pngquant is available (optional)
if command -v pngquant >/dev/null; then
    pngquant --force --skip-if-larger --quality 80-95 --ext .png "${OUT_DIR}"/*.png || true
fi

ls -lh "$OUT_DIR"
rm -rf "$LOG_DIR"
