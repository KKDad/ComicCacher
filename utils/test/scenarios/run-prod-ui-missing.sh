# UI container absent and --ui not given: refuses
source "$(dirname "${BASH_SOURCE[0]}")/_prod-host.inc"
TARGET=host:comics-deploy/run.sh
ARGS=(prod --api 2.6.0)
STDIN=$'y\n'
unset FX_IMAGE_comics_ui FX_DIGEST_comics_ui FX_PROJECT_comics_ui
