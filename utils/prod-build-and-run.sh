#!/bin/bash
#
# Build, push, and deploy comic-api and/or comic-ui to PRODUCTION.
#
# Runs on a workstation (Ubuntu with Podman, or macOS with Docker Desktop):
#   1. prod-build.sh builds and pushes the images (skipped with --skip-build)
#   2. Copies prod-run.sh and utils/prod/docker-compose.yml from this checkout
#      (master, clean) to ${REMOTE_DIR} on the Docker host, so prod always runs
#      the compose file that is on master
#   3. Runs prod-run.sh on the Docker host, which confirms, deploys, checks
#      health and rolls back. All docker commands run there, not from here.
#
# Usage:
#   ./utils/prod-build-and-run.sh --api 2.4.6
#   ./utils/prod-build-and-run.sh --api 2.4.6 --ui 2.4.1
#   ./utils/prod-build-and-run.sh --ui 2.4.1 --skip-build      # use already-pushed image
#   PROD_HOST=root@otherhost ./utils/prod-build-and-run.sh --api 2.4.6
#

set -euo pipefail

PROD_HOST="${PROD_HOST:-root@portainer.stapledon.ca}"
REMOTE_DIR="/root/comics-deploy"
SSH_OPTS="-o ConnectTimeout=10"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${PROJECT_ROOT}/utils/prod/docker-compose.yml"

usage() {
    cat <<EOF
Usage: $0 [--api <version>] [--ui <version>] [--skip-build]

At least one of --api / --ui is required.

  --api <version>   Build, push, and deploy comic-api at <version>
  --ui <version>    Build, push, and deploy comic-ui at <version>
  --skip-build      Skip the build + push; image must already be in registry
  -h, --help        Show this help

Environment overrides:
  PROD_HOST              ssh target of the Docker host (default: ${PROD_HOST})
  COMPOSE_PROJECT_NAME   Passed through to prod-run.sh
EOF
}

die() {
    echo "Error: $*" >&2
    exit 1
}

ARG_API_TAG=""
ARG_UI_TAG=""
SKIP_BUILD=0

while [[ $# -gt 0 ]]; do
    case "$1" in
        --api)
            [[ $# -ge 2 ]] || die "--api requires a version argument"
            ARG_API_TAG="$2"
            shift 2
            ;;
        --ui)
            [[ $# -ge 2 ]] || die "--ui requires a version argument"
            ARG_UI_TAG="$2"
            shift 2
            ;;
        --skip-build)
            SKIP_BUILD=1
            shift
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

if [[ -z "$ARG_API_TAG" && -z "$ARG_UI_TAG" ]]; then
    usage
    die "At least one of --api or --ui is required."
fi

# Validate before the tags are put into the remote command line
SEMVER_REGEX='^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.]+)?$'
for tag in "$ARG_API_TAG" "$ARG_UI_TAG"; do
    if [[ -n "$tag" && ! "$tag" =~ $SEMVER_REGEX ]]; then
        die "'$tag' is not a valid semver tag (expected e.g. 2.4.6 or 2.4.6-rc1)."
    fi
done

# Arguments shared by prod-build.sh and prod-run.sh (bash 3.2: no empty-array expansion under set -u)
DEPLOY_ARGS=""
[[ -n "$ARG_API_TAG" ]] && DEPLOY_ARGS="$DEPLOY_ARGS --api $ARG_API_TAG"
[[ -n "$ARG_UI_TAG" ]] && DEPLOY_ARGS="$DEPLOY_ARGS --ui $ARG_UI_TAG"

# --- Git gate (also for --skip-build: the compose file shipped must be master's) ---
BRANCH=$(git -C "$PROJECT_ROOT" rev-parse --abbrev-ref HEAD)
if [[ "$BRANCH" != "master" ]]; then
    die "Must be on 'master' branch (currently '$BRANCH')."
fi
if [[ -n "$(git -C "$PROJECT_ROOT" status --porcelain)" ]]; then
    die "Working tree is dirty. Commit or stash before deploying."
fi

# --- Step 1: Build + push ---
if [[ $SKIP_BUILD -eq 0 ]]; then
    # shellcheck disable=SC2086  # DEPLOY_ARGS is a validated word list
    "$SCRIPT_DIR/prod-build.sh" $DEPLOY_ARGS
fi

# --- Step 2: Stage prod-run.sh + compose file on the Docker host ---
echo ""
echo "--- Staging prod-run.sh and docker-compose.yml on ${PROD_HOST}:${REMOTE_DIR} ---"
# shellcheck disable=SC2086
ssh $SSH_OPTS "$PROD_HOST" "install -d -m 700 '$REMOTE_DIR'"
# shellcheck disable=SC2086
scp $SSH_OPTS "$SCRIPT_DIR/prod-run.sh" "$COMPOSE_FILE" "${PROD_HOST}:${REMOTE_DIR}/"

# --- Step 3: Deploy on the Docker host (interactive confirm, health check, rollback) ---
echo ""
echo "--- Running prod-run.sh on ${PROD_HOST} ---"
# shellcheck disable=SC2086
exec ssh -t $SSH_OPTS "$PROD_HOST" \
    "COMPOSE_PROJECT_NAME='${COMPOSE_PROJECT_NAME:-}' ${REMOTE_DIR}/prod-run.sh $DEPLOY_ARGS"
