# Second compose deploy: container already belongs to comics-dev, so it isn't removed by hand
TARGET=host:comics-deploy-dev/run.sh
ARGS=(dev --api 2.6.1)
FX_IMAGE_comics_api_dev=registry.stapledon.ca/kkdad/comic-api:2.6.0
FX_PROJECT_comics_api_dev=comics-dev
FX_VOLUMES="comicdata-dev"
FX_HEALTH_comics_api_dev_1="healthy"
setup() {
    printf 'COMICS_DEVTOKEN_ENABLED=true\nCOMICS_DEVTOKEN_SECRET=s\nCOMICS_DEVTOKEN_DEFAULTUSERNAME=uireview0927\n' \
        > "$SB/host/comics-deploy-dev/dev-token.env"
}
