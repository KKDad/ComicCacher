# Push reported success but the registry lacks the image
TARGET=repo:utils/build.sh
ARGS=(prod --api 9.9.9-test)
FX_FAIL=missing-after-push
