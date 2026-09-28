# Rollback uses the compose file of the last successful deploy
source "$(dirname "${BASH_SOURCE[0]}")/_prod-host.inc"
TARGET=host:comics-deploy/run.sh
ARGS=(prod --api 2.6.0)
STDIN=$'y\n'
FX_HEALTH_comics_api_1="unhealthy"
FX_HEALTH_comics_api_2="healthy"
setup() {
    printf 'name: comics\n# previous version\n' > "$SB/host/comics-deploy/docker-compose.last-good.yml"
}
