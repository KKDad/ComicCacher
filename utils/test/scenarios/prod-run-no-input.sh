# No input at the prompt (e.g. stdin swallowed by an earlier ssh)
source "$(dirname "${BASH_SOURCE[0]}")/_prod-host.inc"
TARGET=host:prod-run.sh
ARGS=(--api 2.6.0)
