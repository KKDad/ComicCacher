#!/bin/sh
#
# Build comic-api and push directly to the registry on port 5000 to bypass Cloudflare 100MB upload limits.

set -e

# Your Direct Registry URL (Bypassing Cloudflare)
# Note: Using http since port 5000 is likely unencrypted on the host
DOCKER_REGISTRY="portainer.stapledon.ca:5000"
IMAGE_NAME="kkdad/comic-api"
BUILD_TAG=$1

if [ -z "${BUILD_TAG}" ]; then
   echo "Usage: $0 <tag>"
   echo "Example: $0 2.3.1"
   echo ""
   echo "Current Tags in Registry:"
   curl -s "https://registry.stapledon.ca/v2/${IMAGE_NAME}/tags/list" | jq -r '.tags[]' | sort -V
   exit 1
fi

# 1. Build the image locally with the tag
echo "--- Building ${IMAGE_NAME}:${BUILD_TAG} ---"
docker build -f Dockerfile . --tag "${IMAGE_NAME}:${BUILD_TAG}" --platform linux/amd64

# 2. Push to the registry using Skopeo
# We use the direct port 5000 to ensure large layers succeed.
echo "--- Pushing to ${DOCKER_REGISTRY} via Skopeo ---"
skopeo copy \
    --dest-tls-verify=false \
    docker-daemon:"${IMAGE_NAME}:${BUILD_TAG}" \
    docker://"${DOCKER_REGISTRY}/${IMAGE_NAME}:${BUILD_TAG}"

echo "--- Success! Image ${IMAGE_NAME}:${BUILD_TAG} is now in the registry ---"
