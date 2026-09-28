# Dry run prints the docker commands and changes nothing
TARGET=host:comics-deploy-dev/run.sh
ARGS=(dev --api 2.6.0 --dry-run)
FX_IMAGE_comics_api_dev=registry.stapledon.ca/kkdad/comic-api:2.5.0
