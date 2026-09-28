# run.sh dev next to the prod compose file refuses
source "$(dirname "${BASH_SOURCE[0]}")/_prod-host.inc"
TARGET=host:comics-deploy/run.sh
ARGS=(dev --api 2.6.0)
