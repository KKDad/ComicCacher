# An image without provenance labels (built before them) is refused
TARGET=repo:utils/deploy.sh
ARGS=(prod --ui 9.9.9-test --skip-build)
FX_ON_MASTER=abc1234abc1234abc1234abc1234abc1234abc12
FX_REGISTRY_INFO="kkdad/comic-ui:9.9.9-test=sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb||"
