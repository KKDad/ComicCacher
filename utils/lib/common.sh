# shellcheck shell=bash
# shellcheck disable=SC2034  # the constants and require_env settings are read by the scripts that source this
#
# Shared by the workstation scripts: build.sh, deploy.sh, logs.sh and tunnel.sh.
# Kept compatible with macOS bash 3.2 and BSD tools (no associative arrays, no empty array
# expansion under set -u). Not staged on the Docker host: remote/run.sh is self-contained.
#

UTILS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT_ROOT="$(cd "${UTILS_DIR}/.." && pwd)"

DOCKER_REGISTRY="registry.stapledon.ca"
# Pushes go to the registry's own port: Cloudflare in front of registry.stapledon.ca caps uploads at 100MB
PUSH_REGISTRY="portainer.stapledon.ca:5000"
API_IMAGE="kkdad/comic-api"
UI_IMAGE="kkdad/comic-ui"
DEFAULT_DOCKER_HOST="root@portainer.stapledon.ca"
SSH_OPTS="-o ConnectTimeout=10"
SEMVER_REGEX='^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.]+)?$'

die() {
    echo "Error: $*" >&2
    exit 1
}

# Sets DEPLOY_ENV and the environment's host, remote dir, compose file and containers from the
# first argument, which must be exactly dev or prod. There is no default environment.
require_env() {
    case "${1:-}" in
        dev)
            DEPLOY_ENV="dev"
            DEPLOY_HOST="${DEV_HOST:-$DEFAULT_DOCKER_HOST}"
            REMOTE_DIR="/root/comics-deploy-dev"
            COMPOSE_SOURCE="${UTILS_DIR}/dev/docker-compose.yml"
            API_CONTAINER="comics-api-dev"
            UI_CONTAINER=""
            ;;
        prod)
            DEPLOY_ENV="prod"
            DEPLOY_HOST="${PROD_HOST:-$DEFAULT_DOCKER_HOST}"
            REMOTE_DIR="/root/comics-deploy"
            COMPOSE_SOURCE="${UTILS_DIR}/prod/docker-compose.yml"
            API_CONTAINER="comics-api"
            UI_CONTAINER="comics-ui"
            ;;
        *)
            die "The first argument must be the environment, dev or prod (got '${1:-}')."
            ;;
    esac
}

# Prod checks are skipped only for exactly "dev", so they can't be lost to an unexpected value
is_dev() {
    [[ "${DEPLOY_ENV:-}" == "dev" ]]
}

# Validates --api / --ui versions: at least one, semver, and no UI on dev
check_versions() {
    local api="$1"
    local ui="$2"
    if [[ -z "$api" && -z "$ui" ]]; then
        die "At least one of --api or --ui is required."
    fi
    if is_dev && [[ -n "$ui" ]]; then
        die "dev has no UI container; only --api can be deployed."
    fi
    local tag
    for tag in "$api" "$ui"; do
        if [[ -n "$tag" && ! "$tag" =~ $SEMVER_REGEX ]]; then
            die "'$tag' is not a valid semver tag (expected e.g. 2.4.6 or 2.4.6-rc1)."
        fi
    done
}

# Prod builds and deploys start from master with a clean tree (also for deploy --skip-build:
# the compose file shipped must be master's)
require_clean_master() {
    local action="$1"
    local branch
    branch=$(git -C "$PROJECT_ROOT" rev-parse --abbrev-ref HEAD)
    if [[ "$branch" != "master" ]]; then
        die "Must be on 'master' branch (currently '$branch')."
    fi
    if [[ -n "$(git -C "$PROJECT_ROOT" status --porcelain)" ]]; then
        die "Working tree is dirty. Commit or stash before ${action}."
    fi
}

# Runs a command on the Docker host. -n keeps ssh off stdin, so input meant for the
# confirm prompt later isn't swallowed here
remote() {
    # shellcheck disable=SC2086  # SSH_OPTS is a fixed word list
    ssh -n $SSH_OPTS "$DEPLOY_HOST" "$@"
}
