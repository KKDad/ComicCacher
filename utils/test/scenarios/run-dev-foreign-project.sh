# A comics-api-dev container from another compose project (here prod's) is left alone
TARGET=host:comics-deploy-dev/run.sh
ARGS=(dev --api 2.6.0)
FX_IMAGE_comics_api_dev=registry.stapledon.ca/kkdad/comic-api:2.5.0
FX_PROJECT_comics_api_dev=comics
