# Refuses to deploy from a dirty tree
TARGET=repo:utils/deploy.sh
ARGS=(prod --api 9.9.9-test --skip-build)
FX_DIRTY="?? scratch.txt"
