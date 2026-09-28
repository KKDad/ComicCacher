# Refuses to replace an unlabelled tag, which may be an old release
TARGET=repo:utils/build.sh
ARGS=(prod --api 9.9.9-test)
FX_ON_MASTER=abc1234abc1234abc1234abc1234abc1234abc12
FX_REGISTRY_INFO="kkdad/comic-api:9.9.9-test=sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb||"
