#!/bin/bash
#
# Build, push, and deploy a dev instance of comic-api.
#
# This script:
#   1. Calls build-docker.sh to build the JAR, Docker image, and push to registry
#   2. Deploys a dev container (comics-api-dev) on the remote Docker host via Docker context
#
# Prerequisites:
#   - Docker context "portainer" must be configured (pointing to the remote host)
#   - Skopeo must be installed (used by build-docker.sh for registry push)
#
# Usage: ./utils/dev-build-and-run.sh <version>
# Example: ./utils/dev-build-and-run.sh 2.4.0
#

set -e

DOCKER_CONTEXT="portainer"
DOCKER_REGISTRY="registry.stapledon.ca"
IMAGE_NAME="kkdad/comic-api"
DEV_CONTAINER_NAME="comics-api-dev"
VOLUME_NAME="comicdata-dev"
BUILD_TAG=$1

if [ -z "${BUILD_TAG}" ]; then
    echo "Usage: $0 <version>"
    echo "Example: $0 2.4.0"
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
FULL_IMAGE="${DOCKER_REGISTRY}/${IMAGE_NAME}:${BUILD_TAG}"

# --- Step 1: Build and push via existing build-docker.sh ---
echo "--- [1/2] Building and pushing ${FULL_IMAGE} ---"
"${PROJECT_ROOT}/comic-api/build-docker.sh" "${BUILD_TAG}"

# --- Step 2: Deploy dev container via Docker context ---
echo "--- [2/2] Deploying ${DEV_CONTAINER_NAME} via Docker context '${DOCKER_CONTEXT}' ---"

# Stop and remove existing dev container if present
if docker --context "${DOCKER_CONTEXT}" ps -aq -f name="^${DEV_CONTAINER_NAME}$" | grep -q .; then
    echo "Stopping existing ${DEV_CONTAINER_NAME}..."
    docker --context "${DOCKER_CONTEXT}" stop "${DEV_CONTAINER_NAME}" 2>/dev/null || true
    docker --context "${DOCKER_CONTEXT}" rm "${DEV_CONTAINER_NAME}" 2>/dev/null || true
fi

# Pull the latest image on the remote host
docker --context "${DOCKER_CONTEXT}" pull "${FULL_IMAGE}"

# Create the NFS volume if it doesn't already exist
if ! docker --context "${DOCKER_CONTEXT}" volume ls -q | grep -q "^${VOLUME_NAME}$"; then
    echo "Creating NFS volume ${VOLUME_NAME}..."
    docker --context "${DOCKER_CONTEXT}" volume create \
        --driver local \
        --opt type=nfs4 \
        --opt "o=addr=10.0.0.48,rsize=1048576,wsize=1048576,timeo=600,retrans=2,noresvport,rw,noatime,nconnect=16,vers=4.1" \
        --opt "device=:/volume1/PodGeneral/comics-dev" \
        "${VOLUME_NAME}"
fi

# Start the dev container
docker --context "${DOCKER_CONTEXT}" run -d \
    --name "${DEV_CONTAINER_NAME}" \
    --hostname "${DEV_CONTAINER_NAME}" \
    --restart unless-stopped \
    --network stapledon-network \
    -p 8087:8888 \
    -v "${VOLUME_NAME}:/comics" \
    -e CACHE_DIRECTORY=/comics \
    -e COMICS_CACHE_LOCATION=/comics \
    "${FULL_IMAGE}"

echo ""
echo "--- Done! Dev instance deployed ---"
echo "  Container: ${DEV_CONTAINER_NAME}"
echo "  Image:     ${FULL_IMAGE}"
echo "  Port:      8087"
echo "  Logs:      docker --context ${DOCKER_CONTEXT} logs -f ${DEV_CONTAINER_NAME}"
