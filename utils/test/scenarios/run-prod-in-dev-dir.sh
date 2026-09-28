# run.sh prod next to the dev compose file refuses
source "$(dirname "${BASH_SOURCE[0]}")/_prod-host.inc"
TARGET=host:comics-deploy-dev/run.sh
ARGS=(prod --api 2.6.0)
STDIN=$'y\n'
