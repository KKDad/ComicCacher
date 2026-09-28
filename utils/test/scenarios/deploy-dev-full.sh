# Build and deploy the dev API from a feature branch (no gates)
TARGET=repo:utils/deploy.sh
ARGS=(dev --api 9.9.9-test)
FX_BRANCH=feature/x
FX_DIRTY=" M README.md"
