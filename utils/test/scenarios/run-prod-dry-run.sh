# Dry run prints the plan and changes nothing
source "$(dirname "${BASH_SOURCE[0]}")/_prod-host.inc"
TARGET=host:comics-deploy/run.sh
ARGS=(prod --api 2.6.0 --ui 2.6.0 --dry-run)
