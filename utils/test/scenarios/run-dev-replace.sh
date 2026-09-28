# Replace a running dev container; dev-token.env predates the default username
TARGET=host:comics-deploy-dev/run.sh
ARGS=(dev --api 2.6.0)
FX_IMAGE_comics_api_dev=registry.stapledon.ca/kkdad/comic-api:2.5.0
FX_VOLUMES="comicdata comicdata-dev"
FX_HEALTH_comics_api_dev_1="healthy"
setup() {
    printf 'COMICS_DEVTOKEN_ENABLED=true\nCOMICS_DEVTOKEN_SECRET=existing-secret-kept-as-is\n' \
        > "$SB/host/comics-deploy/dev-token.env"
}
