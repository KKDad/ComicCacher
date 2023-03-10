#!/bin/sh
#
# Build comics-api and push to the stapledon docker registry

set -e

BUILD_TAG=$1
if [ -z $BUILD_TAG ]; then
   echo "Build Tag is required"
   exit
fi

DOCKER_REGISTRY=registry.stapledon.ca


docker build -f Dockerfile . --tag kkdad/comics-api:$BUILD_TAG --platform linux/amd64
docker tag kkdad/comics-api:$BUILD_TAG $DOCKER_REGISTRY:5000/kkdad/comic-api:$BUILD_TAG
docker push $DOCKER_REGISTRY:5000/kkdad/comic-api:$BUILD_TAG
