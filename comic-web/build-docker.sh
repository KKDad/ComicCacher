#!/bin/sh
#
# Build comics-ui and push to the stapledon docker registry

set -e

DOCKER_REGISTRY=registry.stapledon.ca
BUILD_TAG=$1
if [ -z $BUILD_TAG ]; then
   echo "Build Tag is required"
   # Get a sorted list of the current version tags in the repository, limit to 10
    echo "Current Tags for kkdad/comics-ui:"
    curl -s "https://${DOCKER_REGISTRY}/v2/kkdad/comic-ui/tags/list" | jq -r '.tags[]' | sort -rV | head -n 10
   exit
fi

docker build -f Dockerfile . --tag kkdad/comics-ui:$BUILD_TAG --platform linux/amd64
docker tag kkdad/comics-ui:$BUILD_TAG $DOCKER_REGISTRY/kkdad/comic-ui:$BUILD_TAG
docker push $DOCKER_REGISTRY/kkdad/comic-ui:$BUILD_TAG
