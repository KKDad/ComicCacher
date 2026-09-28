# Skip the build: stage and deploy an already-pushed UI
TARGET=repo:utils/prod-build-and-run.sh
ARGS=(--ui 9.9.9-test --skip-build)
