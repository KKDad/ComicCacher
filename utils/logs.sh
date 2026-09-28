#!/bin/bash
#
# Print the recent Docker logs of a ComicCacher container on dev or prod. Read-only: runs
# `docker logs` on the Docker host over ssh and nothing else. For a full health report
# (batch logs, status JSON, dev vs prod) use the comiccacher-logs skill.
#
# Usage:
#   ./utils/logs.sh prod                 # comics-api, last 500 lines
#   ./utils/logs.sh prod ui 2000         # comics-ui
#   ./utils/logs.sh dev api all          # comics-api-dev, everything
#

set -euo pipefail

# shellcheck source-path=SCRIPTDIR source=lib/common.sh
. "$(cd "$(dirname "$0")" && pwd)/lib/common.sh"

usage() {
    cat <<EOF
Usage: $0 <dev|prod> [api|ui] [lines]

  api     comic-api container (default)
  ui      comic-ui container (prod only)
  lines   number of lines, or "all" (default: 500)
EOF
}

case "${1:-}" in
    -h|--help) usage; exit 0 ;;
esac
require_env "${1:-}"

SERVICE="${2:-api}"
LINES="${3:-500}"

case "$SERVICE" in
    api) CONTAINER="$API_CONTAINER" ;;
    ui)
        [[ -n "$UI_CONTAINER" ]] || die "$DEPLOY_ENV has no UI container."
        CONTAINER="$UI_CONTAINER"
        ;;
    *) usage; die "Unknown service: $SERVICE" ;;
esac
[[ "$LINES" =~ ^([0-9]+|all)$ ]] || die "lines must be a number or \"all\" (got '$LINES')."

echo "--- Last $LINES lines of $CONTAINER on $DEPLOY_HOST ---" >&2
# shellcheck disable=SC2029  # validated above, expanded here on purpose
remote "docker logs --tail $LINES $CONTAINER 2>&1"
