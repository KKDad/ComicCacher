# Pull fails (tag missing from the registry): nothing is recreated
source "$(dirname "${BASH_SOURCE[0]}")/_prod-host.inc"
TARGET=host:comics-deploy/run.sh
ARGS=(prod --api 2.6.0)
STDIN=$'y\n'
FX_FAIL=pull
