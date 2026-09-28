# Only exactly dev or prod is accepted
source "$(dirname "${BASH_SOURCE[0]}")/_prod-host.inc"
TARGET=host:comics-deploy/run.sh
ARGS=(PROD --api 2.6.0)
