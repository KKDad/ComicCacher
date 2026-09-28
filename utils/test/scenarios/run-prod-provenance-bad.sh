# --provenance only takes verified or override
source "$(dirname "${BASH_SOURCE[0]}")/_prod-host.inc"
TARGET=host:comics-deploy/run.sh
ARGS=(prod --api 2.6.0 --provenance trusted)
