# API not running before the deploy and unhealthy after: nothing to roll back to
source "$(dirname "${BASH_SOURCE[0]}")/_prod-host.inc"
TARGET=host:prod-run.sh
ARGS=(--api 2.6.0)
STDIN=$'y\n'
unset FX_IMAGE_comics_api FX_DIGEST_comics_api FX_PROJECT_comics_api
FX_HEALTH_comics_api_1="unhealthy"
