# Build and push API and UI from clean master; both found in the registry
TARGET=repo:utils/prod-build.sh
ARGS=(--api 9.9.9-test --ui 9.9.9-test)
FX_REGISTRY_HAS="kkdad/comic-api:9.9.9-test kkdad/comic-ui:9.9.9-test"
