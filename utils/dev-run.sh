#!/bin/bash
#
# Deploy the comic-api DEV instance (comics-api-dev) on the Docker host.
#
# Runs ON the Docker host with its local `docker`. The workstation stages this
# script with dev-build-and-run.sh (or by hand); nothing in this script builds images.
#
# Steps:
#   - Pull the image first, so a tag missing from the registry fails before anything is removed
#   - Create the comicdata-dev NFS volume if it doesn't exist
#   - Replace comics-api-dev with a container running the new image
#   - Poll Docker health status. There is no rollback: this is dev, and a failed
#     container is left running so its logs can be read
#
# Usage:
#   ./dev-run.sh 2.4.8
#   ./dev-run.sh 2.4.8 --dry-run      # print the docker commands, change nothing
#

set -euo pipefail

# --- Constants ---
DOCKER_REGISTRY="registry.stapledon.ca"
IMAGE_NAME="kkdad/comic-api"
DEV_CONTAINER_NAME="comics-api-dev"
VOLUME_NAME="comicdata-dev"
HEALTH_TIMEOUT_SECS=300
HEALTH_POLL_INTERVAL=5
SEMVER_REGEX='^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.]+)?$'

usage() {
    cat <<EOF
Usage: $0 <version> [--dry-run]

Run on the Docker host. Replaces ${DEV_CONTAINER_NAME} with ${DOCKER_REGISTRY}/${IMAGE_NAME}:<version>.

  --dry-run     Print the docker commands, then exit without changing anything
  -h, --help    Show this help
EOF
}

die() {
    echo "Error: $*" >&2
    exit 1
}

# --- Arg parse ---
BUILD_TAG=""
DRY_RUN=0

while [[ $# -gt 0 ]]; do
    case "$1" in
        --dry-run)
            DRY_RUN=1
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
[[ "$BUILD_TAG" =~ $SEMVER_REGEX ]] || die "'$BUILD_TAG' is not a valid semver tag (expected e.g. 2.4.8 or 2.4.8-rc1)."

FULL_IMAGE="${DOCKER_REGISTRY}/${IMAGE_NAME}:${BUILD_TAG}"
CURRENT_IMAGE=$(docker inspect --format '{{ .Config.Image }}' "$DEV_CONTAINER_NAME" 2>/dev/null || true)

echo ""
echo "--- Dev deploy: ${DEV_CONTAINER_NAME} on $(hostname) ---"
echo "  Current: ${CURRENT_IMAGE:-<none>}"
echo "  Target:  ${FULL_IMAGE}"

RUN_ARGS=(
    -d
    --name "$DEV_CONTAINER_NAME"
    --hostname "$DEV_CONTAINER_NAME"
    --restart unless-stopped
    --network stapledon-network
    -p 8087:8888
    -v "${VOLUME_NAME}:/comics"
    -e CACHE_DIRECTORY=/comics
    -e COMICS_CACHE_LOCATION=/comics
    "$FULL_IMAGE"
)

if [[ $DRY_RUN -eq 1 ]]; then
    echo ""
    echo "--- Would run ---"
    echo "  docker pull $FULL_IMAGE"
    echo "  docker volume create ... $VOLUME_NAME   (only if missing)"
    [[ -n "$CURRENT_IMAGE" ]] && echo "  docker stop $DEV_CONTAINER_NAME && docker rm $DEV_CONTAINER_NAME"
    echo "  docker run ${RUN_ARGS[*]}"
    echo ""
    echo "Dry run: nothing changed."
    exit 0
fi

# --- Pull first: fail before touching the running container ---
echo ""
echo "--- Pulling ${FULL_IMAGE} ---"
docker pull "$FULL_IMAGE"

# --- Create the NFS volume if it doesn't already exist ---
if ! docker volume ls -q | grep -q "^${VOLUME_NAME}$"; then
    echo "Creating NFS volume ${VOLUME_NAME}..."
    docker volume create \
        --driver local \
        --opt type=nfs4 \
        --opt "o=addr=10.0.0.48,rsize=1048576,wsize=1048576,timeo=600,retrans=2,noresvport,rw,noatime,nconnect=16,vers=4.1" \
        --opt "device=:/volume1/PodGeneral/comics-dev" \
        "$VOLUME_NAME"
fi

# --- Replace the container ---
if [[ -n "$CURRENT_IMAGE" ]]; then
    echo "Stopping existing ${DEV_CONTAINER_NAME}..."
    docker stop "$DEV_CONTAINER_NAME" >/dev/null
    docker rm "$DEV_CONTAINER_NAME" >/dev/null
fi

echo "Starting ${DEV_CONTAINER_NAME}..."
docker run "${RUN_ARGS[@]}" >/dev/null

# --- Health poll ---
echo ""
echo "--- Polling health (timeout ${HEALTH_TIMEOUT_SECS}s) ---"
elapsed=0
while (( elapsed < HEALTH_TIMEOUT_SECS )); do
    status=$(docker inspect \
        --format '{{ if .State.Health }}{{ .State.Health.Status }}{{ else }}no-healthcheck{{ end }}' \
        "$DEV_CONTAINER_NAME" 2>/dev/null || echo "missing")
    printf "  [t=%3ds] %-15s %s\n" "$elapsed" "$DEV_CONTAINER_NAME" "$status"
    if [[ "$status" == "healthy" ]]; then
        echo ""
        echo "Dev deploy successful."
        echo "  Container: ${DEV_CONTAINER_NAME}"
        echo "  Image:     ${FULL_IMAGE}"
        echo "  Port:      8087"
        echo "  Logs:      docker logs -f ${DEV_CONTAINER_NAME}"
        exit 0
    fi
    sleep "$HEALTH_POLL_INTERVAL"
    elapsed=$(( elapsed + HEALTH_POLL_INTERVAL ))
done

echo ""
echo "${DEV_CONTAINER_NAME} is not healthy after ${HEALTH_TIMEOUT_SECS}s; it is left running."
echo "Startup catch-up jobs can hold readiness down for a while (see TODO.md)."
echo "  Logs: docker logs --tail 100 ${DEV_CONTAINER_NAME}"
exit 1
