# --allow-unverified-image deploys it, pinned, with a warning and provenance=override
TARGET=repo:utils/deploy.sh
ARGS=(prod --ui 9.9.9-test --skip-build --allow-unverified-image)
FX_ON_MASTER=abc1234abc1234abc1234abc1234abc1234abc12
FX_REGISTRY_INFO="kkdad/comic-ui:9.9.9-test=sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb||"
