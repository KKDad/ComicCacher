# COMPOSE_PROJECT_NAME is passed through to prod-run.sh
TARGET=repo:utils/prod-build-and-run.sh
ARGS=(--api 9.9.9-test --skip-build)
COMPOSE_PROJECT_NAME=comics2
