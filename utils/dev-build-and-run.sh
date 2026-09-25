#!/bin/bash
#
# Build, push, and deploy a dev instance of comic-api.
#
# Runs on a workstation (Ubuntu with Podman, or macOS with Docker Desktop):
#   1. Calls build-docker.sh to build the JAR and image and push it to the registry
#      (skipped with --skip-build)
#   2. Copies dev-run.sh to ${REMOTE_DIR} on the Docker host
#   3. Runs dev-run.sh on the Docker host, which replaces comics-api-dev and
#      checks health. All docker commands run there, not from here.
#
# Prerequisites:
#   - ssh access to the Docker host
#   - Skopeo must be installed (used by build-docker.sh for registry push)
#
# Usage:
#   ./utils/dev-build-and-run.sh 2.4.8
#   ./utils/dev-build-and-run.sh 2.4.8 --skip-build    # use already-pushed image
#   DEV_HOST=root@otherhost ./utils/dev-build-and-run.sh 2.4.8
#

set -euo pipefail

DEV_HOST="${DEV_HOST:-root@portainer.stapledon.ca}"
REMOTE_DIR="/root/comics-deploy"
SSH_OPTS="-o ConnectTimeout=10"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

usage() {
    cat <<EOF
Usage: $0 <version> [--skip-build]

  --skip-build   Skip the build + push; image must already be in registry
  -h, --help     Show this help

Environment overrides:
  DEV_HOST   ssh target of the Docker host (default: ${DEV_HOST})
EOF
}

die() {
    echo "Error: $*" >&2
    exit 1
}

BUILD_TAG=""
SKIP_BUILD=0

while [[ $# -gt 0 ]]; do
    case "$1" in
        --skip-build)
            SKIP_BUILD=1
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        -*)
            usage
            die "Unknown argument: $1"
            ;;
        *)
            [[ -z "$BUILD_TAG" ]] || die "Only one version may be given"
            BUILD_TAG="$1"
            shift
            ;;
    esac
done

[[ -n "$BUILD_TAG" ]] || { usage; die "A version is required."; }

# Validate before the tag is put into the remote command line
SEMVER_REGEX='^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.]+)?$'
[[ "$BUILD_TAG" =~ $SEMVER_REGEX ]] || die "'$BUILD_TAG' is not a valid semver tag (expected e.g. 2.4.8 or 2.4.8-rc1)."

# --- Step 1: Build + push ---
if [[ $SKIP_BUILD -eq 0 ]]; then
    echo "--- [1/3] Building and pushing comic-api ${BUILD_TAG} ---"
    "${PROJECT_ROOT}/comic-api/build-docker.sh" "${BUILD_TAG}"
else
    echo "--- [1/3] Skipping build (--skip-build) ---"
fi

# --- Step 2: Stage dev-run.sh on the Docker host ---
echo ""
echo "--- [2/3] Staging dev-run.sh on ${DEV_HOST}:${REMOTE_DIR} ---"
# shellcheck disable=SC2086
ssh -n $SSH_OPTS "$DEV_HOST" "install -d -m 700 '$REMOTE_DIR'"
# shellcheck disable=SC2086
scp $SSH_OPTS "$SCRIPT_DIR/dev-run.sh" "${DEV_HOST}:${REMOTE_DIR}/"

# --- Step 3: Deploy on the Docker host ---
echo ""
echo "--- [3/3] Running dev-run.sh on ${DEV_HOST} ---"
# shellcheck disable=SC2086
exec ssh $SSH_OPTS "$DEV_HOST" "${REMOTE_DIR}/dev-run.sh ${BUILD_TAG}"
