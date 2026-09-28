# Build, stage and deploy the API; the y piped in shows which ssh reads it
TARGET=repo:utils/deploy.sh
ARGS=(prod --api 9.9.9-test)
STDIN=$'y\n'
FX_REGISTRY_HAS="kkdad/comic-api:9.9.9-test"
