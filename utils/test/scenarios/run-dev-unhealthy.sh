# New dev container never gets healthy: left running, no rollback
TARGET=host:comics-deploy-dev/run.sh
ARGS=(dev --api 2.6.0)
FX_IMAGE_comics_api_dev=registry.stapledon.ca/kkdad/comic-api:2.5.0
FX_VOLUMES="comicdata-dev"
FX_HEALTH_comics_api_dev_1="unhealthy"
setup() {
    printf 'COMICS_DEVTOKEN_ENABLED=true\nCOMICS_DEVTOKEN_SECRET=s\nCOMICS_DEVTOKEN_DEFAULTUSERNAME=uireview0927\n' \
        > "$SB/host/comics-deploy/dev-token.env"
}
