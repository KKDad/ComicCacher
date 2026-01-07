#!/bin/sh
#
# Build comics-ui and push directly to the registry on port 5000 to bypass Cloudflare 100MB upload limits.

set -e

# Your Direct Registry URL (Bypassing Cloudflare)
# Note: Using http since port 5000 is likely unencrypted on the host
DOCKER_REGISTRY="portainer.stapledon.ca:5000"
IMAGE_NAME="kkdad/comic-ui"
BUILD_TAG=$1

if [ -z "${BUILD_TAG}" ]; then
   echo "Usage: $0 <tag>"
   echo "Example: $0 2.3.1"
   echo ""
   echo "Current Tags in Registry:"
   curl -s "https://registry.stapledon.ca/v2/${IMAGE_NAME}/tags/list" | jq -r '.tags[]' | sort -V
   exit 1
fi

# 1. Build the Docker image locally with the tag
FULL_IMAGE="${DOCKER_REGISTRY}/${IMAGE_NAME}:${BUILD_TAG}"
echo "--- Building ${FULL_IMAGE} ---"
docker build -f Dockerfile . --tag "${FULL_IMAGE}" --platform linux/amd64

# 2. Push to the registry using Skopeo
# We use the direct port 5000 to bypass Cloudflare's 100MB chunked upload limit.
# Save image to tarball first since Skopeo's docker-daemon transport has issues with Docker Desktop.
echo "--- Saving image to tarball ---"
TARBALL="/tmp/comic-ui-${BUILD_TAG}.tar"
docker save "${FULL_IMAGE}" -o "${TARBALL}"

echo "--- Pushing ${FULL_IMAGE} via Skopeo ---"
skopeo copy \
    --dest-tls-verify=false \
    docker-archive:"${TARBALL}" \
    docker://"${FULL_IMAGE}"

# Clean up tarball
rm -f "${TARBALL}"

echo "--- Success! Image ${FULL_IMAGE} is now in the registry ---"
