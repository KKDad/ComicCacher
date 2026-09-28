# compose up fails after the pull
source "$(dirname "${BASH_SOURCE[0]}")/_prod-host.inc"
TARGET=host:prod-run.sh
ARGS=(--api 2.6.0)
STDIN=$'y\n'
FX_FAIL=up
FX_HEALTH_comics_api_1="unhealthy"
FX_HEALTH_comics_api_2="healthy"
