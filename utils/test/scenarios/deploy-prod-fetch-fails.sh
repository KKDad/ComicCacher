# Can't fetch origin/master: refuses rather than check against a stale ref
TARGET=repo:utils/deploy.sh
ARGS=(prod --api 9.9.9-test --skip-build)
FX_FAIL=fetch
FX_ON_MASTER=abc1234abc1234abc1234abc1234abc1234abc12
FX_REGISTRY_INFO="kkdad/comic-api:9.9.9-test=sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb|abc1234abc1234abc1234abc1234abc1234abc12|false"
