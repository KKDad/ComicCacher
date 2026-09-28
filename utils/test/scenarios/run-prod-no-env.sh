# run.sh without an environment refuses before touching docker
source "$(dirname "${BASH_SOURCE[0]}")/_prod-host.inc"
TARGET=host:comics-deploy/run.sh
ARGS=(--api 2.6.0)
