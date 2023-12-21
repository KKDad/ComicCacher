#!/bin/sh
#
# Build comics-api and push to the stapledon docker registry

set -e

DOCKER_REGISTRY=registry.stapledon.ca
BUILD_TAG=$1
if [ -z $BUILD_TAG ]; then
   echo "Build Tag is required"
   # Get a sorted list of the current tags in the repository
   echo "Current Tags for kkdad/photo-organizer-api:"
   curl -s "https://${DOCKER_REGISTRY}/v2/kkdad/comic-api/tags/list" | jq -r '.tags[]' | sort -r
   exit
fi

docker build -f Dockerfile . --tag kkdad/comics-api:$BUILD_TAG --platform linux/amd64
docker tag kkdad/comics-api:$BUILD_TAG $DOCKER_REGISTRY:5000/kkdad/comic-api:$BUILD_TAG
docker push $DOCKER_REGISTRY:5000/kkdad/comic-api:$BUILD_TAG
