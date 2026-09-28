# Running image is digest-pinned; deploy another pinned ref
source "$(dirname "${BASH_SOURCE[0]}")/_prod-host.inc"
TARGET=host:comics-deploy/run.sh
ARGS=(prod --api 2.6.0@sha256:9999999999999999999999999999999999999999999999999999999999999999)
STDIN=$'y\n'
FX_IMAGE_comics_api=registry.stapledon.ca/kkdad/comic-api:2.5.0@sha256:1111111111111111111111111111111111111111111111111111111111111111
FX_HEALTH_comics_api_1="healthy"
