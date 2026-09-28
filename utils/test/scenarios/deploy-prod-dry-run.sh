# Dry run: no build, stage the files, run.sh --dry-run
TARGET=repo:utils/deploy.sh
ARGS=(prod --api 9.9.9-test --dry-run)
FX_ON_MASTER=abc1234abc1234abc1234abc1234abc1234abc12
FX_REGISTRY_INFO="kkdad/comic-api:9.9.9-test=sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb|abc1234abc1234abc1234abc1234abc1234abc12|false"
