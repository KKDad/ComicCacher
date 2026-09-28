#!/bin/bash
#
# Open an ssh tunnel from localhost:8888 to the production API on the Docker host, e.g. to
# run the UI against prod: ./utils/dev-ui.sh --api http://localhost:8888/graphql
# Prod only: the dev API is already reachable at http://portainer.stapledon.ca:8087.
#
# Stays in the foreground; Control-C closes the tunnel.
#
# Usage:
#   ./utils/tunnel.sh prod
#

set -euo pipefail

# shellcheck source-path=SCRIPTDIR source=lib/common.sh
. "$(cd "$(dirname "$0")" && pwd)/lib/common.sh"

REMOTE_API_PORT=8888
LOCAL_API_PORT=8888

case "${1:-}" in
    -h|--help)
        echo "Usage: $0 prod"
        exit 0
        ;;
esac
require_env "${1:-}"
if is_dev; then
    die "No tunnel needed for dev: its API is at http://portainer.stapledon.ca:8087/graphql."
fi

echo "Opening tunnel: localhost:${LOCAL_API_PORT} -> ${DEPLOY_HOST} localhost:${REMOTE_API_PORT} (prod API)"
# shellcheck disable=SC2086
ssh -N -o ExitOnForwardFailure=yes $SSH_OPTS -L "${LOCAL_API_PORT}:localhost:${REMOTE_API_PORT}" "$DEPLOY_HOST" &
TUNNEL_PID=$!
# Only this ssh is stopped, never another one that happens to match
trap 'kill "$TUNNEL_PID" 2>/dev/null || true; echo ""; echo "Tunnel closed."' EXIT
trap 'exit 0' INT TERM

echo "Press Control-C to close it."
if wait "$TUNNEL_PID"; then
    exit 0
fi
die "The tunnel failed or closed (is port ${LOCAL_API_PORT} already in use?)."
