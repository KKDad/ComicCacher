# Running containers belong to another compose project
source "$(dirname "${BASH_SOURCE[0]}")/_prod-host.inc"
TARGET=host:comics-deploy/run.sh
ARGS=(prod --api 2.6.0)
STDIN=$'y\n'
FX_PROJECT_comics_api=other
FX_PROJECT_comics_ui=other
