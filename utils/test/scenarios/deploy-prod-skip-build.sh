# Skip the build: stage and deploy an already-pushed UI
TARGET=repo:utils/deploy.sh
ARGS=(prod --ui 9.9.9-test --skip-build)
FX_ON_MASTER=abc1234abc1234abc1234abc1234abc1234abc12
FX_REGISTRY_INFO="kkdad/comic-ui:9.9.9-test=sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb|abc1234abc1234abc1234abc1234abc1234abc12|false"
