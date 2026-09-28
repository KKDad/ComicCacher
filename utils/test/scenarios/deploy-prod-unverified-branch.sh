# An image built from a commit that isn't on origin/master is refused before any ssh
TARGET=repo:utils/deploy.sh
ARGS=(prod --api 9.9.9-test --skip-build)
FX_ON_MASTER=abc1234abc1234abc1234abc1234abc1234abc12
FX_REGISTRY_INFO="kkdad/comic-api:9.9.9-test=sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb|fff0000fff0000fff0000fff0000fff0000fff00|false"
