#!/bin/sh
#
# Build comics-ui and push to the stapledon docker registry

set -e

BUILD_TAG=$1
if [ -z $BUILD_TAG ]; then
   echo "Build Tag is required"
   exit
fi

DOCKER_REGISTRY=registry.stapledon.local


docker build -f Dockerfile . --tag kkdad/comics-ui:$BUILD_TAG --platform linux/amd64
docker tag kkdad/comics-ui:$BUILD_TAG $DOCKER_REGISTRY:5000/kkdad/comic-ui:$BUILD_TAG
docker push $DOCKER_REGISTRY:5000/kkdad/comic-ui:$BUILD_TAG
