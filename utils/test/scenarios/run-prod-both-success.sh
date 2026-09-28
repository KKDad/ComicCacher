# Deploy API and UI together
source "$(dirname "${BASH_SOURCE[0]}")/_prod-host.inc"
TARGET=host:comics-deploy/run.sh
ARGS=(prod --api 2.6.0 --ui 2.6.0)
STDIN=$'y\n'
FX_HEALTH_comics_api_1="starting healthy"
FX_HEALTH_comics_ui_1="healthy"
