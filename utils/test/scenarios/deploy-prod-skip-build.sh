# Skip the build: stage and deploy an already-pushed UI
TARGET=repo:utils/deploy.sh
ARGS=(prod --ui 9.9.9-test --skip-build)
