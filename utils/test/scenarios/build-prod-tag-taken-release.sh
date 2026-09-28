# Refuses to replace a tag that holds a build of master
TARGET=repo:utils/build.sh
ARGS=(prod --api 9.9.9-test)
FX_ON_MASTER=abc1234abc1234abc1234abc1234abc1234abc12
FX_REGISTRY_INFO="kkdad/comic-api:9.9.9-test=sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb|abc1234abc1234abc1234abc1234abc1234abc12|false"
