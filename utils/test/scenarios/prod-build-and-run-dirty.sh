# Refuses to deploy from a dirty tree
TARGET=repo:utils/prod-build-and-run.sh
ARGS=(--api 9.9.9-test --skip-build)
FX_DIRTY="?? scratch.txt"
