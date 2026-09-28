# Pull fails: the running dev container is untouched
TARGET=host:comics-deploy-dev/run.sh
ARGS=(dev --api 2.6.0)
FX_IMAGE_comics_api_dev=registry.stapledon.ca/kkdad/comic-api:2.5.0
FX_FAIL=pull
