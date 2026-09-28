#!/bin/bash
#
# Build, push, and deploy ComicCacher to dev or prod.
#
# Runs on a workstation (Ubuntu with Podman, or macOS with Docker Desktop):
#   1. build.sh builds and pushes the images (skipped with --skip-build or --dry-run)
#   2. Copies remote/run.sh and the environment's compose file from this checkout to the
#      Docker host:
#        prod: utils/prod/docker-compose.yml -> /root/comics-deploy
#        dev:  utils/dev/docker-compose.yml  -> /root/comics-deploy-dev
#   3. Runs run.sh there. For prod it confirms, writes the audit log, deploys, checks health
#      and rolls back; for dev it deploys and checks health. All docker commands run there.
#
# prod needs master with a clean tree, also for --skip-build, so prod always runs the
# compose file that is on master. dev deploys from any branch and has no UI container.
#
# Usage:
#   ./utils/deploy.sh prod --api 2.4.6
#   ./utils/deploy.sh prod --api 2.4.6 --ui 2.4.1
#   ./utils/deploy.sh prod --ui 2.4.1 --skip-build      # use an already-pushed image
#   ./utils/deploy.sh prod --api 2.4.6 --dry-run        # stage the files, print the plan, deploy nothing
#   ./utils/deploy.sh dev --api 2.4.8-rc1
#   PROD_HOST=root@otherhost ./utils/deploy.sh prod --api 2.4.6
#

set -euo pipefail

# shellcheck source-path=SCRIPTDIR source=lib/common.sh
. "$(cd "$(dirname "$0")" && pwd)/lib/common.sh"

usage() {
    cat <<EOF
Usage: $0 <dev|prod> [--api <version>] [--ui <version>] [--skip-build] [--dry-run]

At least one of --api / --ui is required (dev: --api only).

  --api <version>   Build, push, and deploy comic-api at <version>
  --ui <version>    Build, push, and deploy comic-ui at <version>
  --skip-build      Skip the build + push; the image must already be in the registry
  --dry-run         Skip the build, stage the files, and show the plan without deploying
  -h, --help        Show this help

Environment overrides:
  PROD_HOST, DEV_HOST    ssh target of the Docker host (default: ${DEFAULT_DOCKER_HOST})
  COMPOSE_PROJECT_NAME   prod only: passed through to run.sh
EOF
}

case "${1:-}" in
    -h|--help) usage; exit 0 ;;
esac
require_env "${1:-}"
shift

ARG_API_TAG=""
ARG_UI_TAG=""
SKIP_BUILD=0
DRY_RUN=0

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
        --dry-run)
            DRY_RUN=1
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

# Validated before the tags are put into the remote command line
check_versions "$ARG_API_TAG" "$ARG_UI_TAG"

# Arguments shared by build.sh and run.sh (bash 3.2: no empty-array expansion under set -u)
VERSION_ARGS=""
[[ -n "$ARG_API_TAG" ]] && VERSION_ARGS="$VERSION_ARGS --api $ARG_API_TAG"
[[ -n "$ARG_UI_TAG" ]] && VERSION_ARGS="$VERSION_ARGS --ui $ARG_UI_TAG"
RUN_ARGS="$VERSION_ARGS"
[[ $DRY_RUN -eq 1 ]] && RUN_ARGS="$RUN_ARGS --dry-run"

if ! is_dev; then
    require_clean_master "deploying"
fi

# --- Step 1: Build + push ---
if [[ $SKIP_BUILD -eq 0 ]]; then
    # shellcheck disable=SC2086  # VERSION_ARGS is a validated word list
    "$UTILS_DIR/build.sh" "$DEPLOY_ENV" $VERSION_ARGS
fi

# --- Step 2: Stage run.sh + compose file on the Docker host ---
echo ""
echo "--- Staging run.sh and docker-compose.yml on ${DEPLOY_HOST}:${REMOTE_DIR} ---"
# shellcheck disable=SC2029  # REMOTE_DIR is a constant, expanded here on purpose
remote "install -d -m 700 '$REMOTE_DIR'"
# shellcheck disable=SC2086
scp $SSH_OPTS "$UTILS_DIR/remote/run.sh" "$COMPOSE_SOURCE" "${DEPLOY_HOST}:${REMOTE_DIR}/" < /dev/null

# --- Step 3: Deploy on the Docker host ---
echo ""
echo "--- Running run.sh ${DEPLOY_ENV} on ${DEPLOY_HOST} ---"
if is_dev; then
    # shellcheck disable=SC2086
    exec ssh $SSH_OPTS "$DEPLOY_HOST" "${REMOTE_DIR}/run.sh dev${RUN_ARGS}"
fi
# -t gives run.sh a terminal for its confirm prompt
# shellcheck disable=SC2086
exec ssh -t $SSH_OPTS "$DEPLOY_HOST" \
    "COMPOSE_PROJECT_NAME='${COMPOSE_PROJECT_NAME:-}' ${REMOTE_DIR}/run.sh prod${RUN_ARGS}"
