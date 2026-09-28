# An image built from a dirty tree is refused, even from a master commit
TARGET=repo:utils/deploy.sh
ARGS=(prod --api 9.9.9-test --skip-build)
FX_ON_MASTER=abc1234abc1234abc1234abc1234abc1234abc12
FX_REGISTRY_INFO="kkdad/comic-api:9.9.9-test=sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb|abc1234abc1234abc1234abc1234abc1234abc12|true"
