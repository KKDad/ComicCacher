# New API never gets healthy; rollback to the baseline digest succeeds
source "$(dirname "${BASH_SOURCE[0]}")/_prod-host.inc"
TARGET=host:prod-run.sh
ARGS=(--api 2.6.0)
STDIN=$'y\n'
FX_HEALTH_comics_api_1="unhealthy"
FX_HEALTH_comics_api_2="healthy"
