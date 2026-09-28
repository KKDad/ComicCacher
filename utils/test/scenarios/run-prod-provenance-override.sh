# run.sh records the provenance deploy.sh passes
source "$(dirname "${BASH_SOURCE[0]}")/_prod-host.inc"
TARGET=host:comics-deploy/run.sh
ARGS=(prod --api 2.6.0@sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb --provenance override)
STDIN=$'y\n'
FX_HEALTH_comics_api_1="healthy"
