#!/bin/bash
#
# Deploy ComicCacher on the Docker host: comic-api and comic-ui for prod, comic-api for dev.
#
# Runs ON the Docker host with its local `docker`, using the docker-compose.yml that sits
# next to this script. The workstation stages both with `utils/deploy.sh <env>`:
#   prod: /root/comics-deploy      (utils/prod/docker-compose.yml, compose project "comics")
#   dev:  /root/comics-deploy-dev  (utils/dev/docker-compose.yml, compose project "comics-dev")
# Self-contained on purpose (sources nothing), so it can also be staged and run by hand.
# Run by hand, it skips the workstation's checks (master branch, clean tree, image provenance).
#
# Steps:
#   - The environment must match the compose file beside this script (its `name:`),
#     so `run.sh dev` in the prod directory, or the reverse, changes nothing
#   - Capture the running image of each container, by digest, as the rollback baseline
#   - prod: compose project label on running containers must match the compose project
#   - prod: operator confirmation prompt, and an audit log entry in ~/.comiccacher-prod-deploy.log
#   - compose pull, then up -d --no-deps for the changed services only
#   - Poll Docker health status
#   - prod: if up or the health check fails, roll back to the baseline digests with the
#     compose file of the last successful deploy (docker-compose.last-good.yml)
#   - dev: no rollback; a failed container is left running so its logs can be read.
#     Creates the comicdata-dev volume and dev-token.env if missing (see dev_prepare)
#
# Usage:
#   ./run.sh prod --api 2.4.6
#   ./run.sh prod --api 2.4.6@sha256:<64 hex>        # pin the exact image
#   ./run.sh prod --api 2.4.6 --ui 2.4.1 --dry-run     # show the plan, change nothing
#   ./run.sh dev --api 2.4.8
#

set -euo pipefail

# --- Constants ---
DOCKER_REGISTRY="registry.stapledon.ca"
API_IMAGE="kkdad/comic-api"
UI_IMAGE="kkdad/comic-ui"
# semver, optionally pinned by digest: 2.4.6, 2.4.6-rc1, 2.4.6@sha256:<64 hex>
REF_REGEX='^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.]+)?(@sha256:[0-9a-f]{64})?$'
AUDIT_LOG="${HOME}/.comiccacher-prod-deploy.log"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.yml"
LAST_GOOD_COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.last-good.yml"

usage() {
    cat <<EOF
Usage: $0 <dev|prod> [--api <ref>] [--ui <ref>] [--dry-run]

Run on the Docker host. At least one of --api / --ui is required (dev: --api only).
<ref> is a semver tag (2.4.6) or a tag pinned by digest (2.4.6@sha256:...).

  --api <ref>   Deploy comic-api at <ref>
  --ui <ref>    Deploy comic-ui at <ref>
  --dry-run     Print the plan, the rendered compose config and the docker
                commands, then exit without changing anything
  -h, --help    Show this help

Compose file: ${COMPOSE_FILE}

Environment overrides:
  COMPOSE_PROJECT_NAME   prod only: override the compose project name (default: comics)
EOF
}

die() {
    echo "Error: $*" >&2
    exit 1
}

# --- Step 1: Environment, arg parse + ref validation ---
case "${1:-}" in
    -h|--help)
        usage
        exit 0
        ;;
    dev)
        DEPLOY_ENV="dev"
        PLAN_TITLE="Dev Deploy Plan"
        COMPOSE_PROJECT_DEFAULT="comics-dev"
        API_CONTAINER="comics-api-dev"
        UI_CONTAINER=""
        API_PORT=8087
        HEALTH_TIMEOUT_SECS=300
        HEALTH_POLL_INTERVAL=5
        VOLUME_NAME="comicdata-dev"
        DEV_TOKEN_ENV="${SCRIPT_DIR}/dev-token.env"
        # Where dev-run.sh kept it, beside the prod files; copied once so the secret stays the same
        LEGACY_DEV_TOKEN_ENV="${SCRIPT_DIR}/../comics-deploy/dev-token.env"
        # devToken issues tokens for this user when no username is given. A USER-role test
        # account on the dev data, so a default token isn't an admin login
        DEV_TOKEN_DEFAULT_USERNAME="uireview0927"
        ;;
    prod)
        DEPLOY_ENV="prod"
        PLAN_TITLE="Production Deploy Plan"
        COMPOSE_PROJECT_DEFAULT="comics"
        API_CONTAINER="comics-api"
        UI_CONTAINER="comics-ui"
        API_PORT=8888
        UI_PORT=8899
        HEALTH_TIMEOUT_SECS=180
        HEALTH_POLL_INTERVAL=3
        ;;
    *)
        usage
        die "The first argument must be the environment, dev or prod (got '${1:-}')."
        ;;
esac
shift

# Prod safety steps are skipped only for exactly "dev", so they can't be lost to a typo
is_dev() {
    [[ "$DEPLOY_ENV" == "dev" ]]
}

ARG_API_REF=""
ARG_UI_REF=""
DRY_RUN=0

while [[ $# -gt 0 ]]; do
    case "$1" in
        --api)
            [[ $# -ge 2 ]] || die "--api requires a version argument"
            ARG_API_REF="$2"
            shift 2
            ;;
        --ui)
            [[ $# -ge 2 ]] || die "--ui requires a version argument"
            ARG_UI_REF="$2"
            shift 2
            ;;
        --dry-run)
            DRY_RUN=1
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            usage
            die "Unknown argument: $1"
            ;;
    esac
done

if [[ -z "$ARG_API_REF" && -z "$ARG_UI_REF" ]]; then
    usage
    die "At least one of --api or --ui is required."
fi
if is_dev && [[ -n "$ARG_UI_REF" ]]; then
    die "dev has no UI container; only --api can be deployed."
fi

for ref in "$ARG_API_REF" "$ARG_UI_REF"; do
    if [[ -n "$ref" && ! "$ref" =~ $REF_REGEX ]]; then
        die "'$ref' is not a valid ref (expected e.g. 2.4.6, 2.4.6-rc1 or 2.4.6@sha256:<64 hex>)."
    fi
done

[[ -f "$COMPOSE_FILE" ]] || die "Compose file not found: $COMPOSE_FILE"

# The compose file names its project; it must be this environment's
COMPOSE_FILE_PROJECT=$(sed -n 's/^name:[[:space:]]*//p' "$COMPOSE_FILE" | head -1 | tr -d "\"' ")
if [[ "$COMPOSE_FILE_PROJECT" != "$COMPOSE_PROJECT_DEFAULT" ]]; then
    die "$COMPOSE_FILE is for compose project '${COMPOSE_FILE_PROJECT:-<none>}', not ${DEPLOY_ENV} ('$COMPOSE_PROJECT_DEFAULT'). Wrong directory?"
fi

# --- Step 2: Capture current state from running containers ---
inspect_field() {
    local container="$1"
    local format="$2"
    docker inspect --format "$format" "$container" 2>/dev/null || true
}

# The running image as <tag>@<digest>, so a rollback restores the exact image
# even if the tag has since been moved in the registry.
current_ref() {
    local container="$1"
    local repo="$2"
    local config_image image_id tag digest
    config_image=$(inspect_field "$container" '{{ .Config.Image }}')
    [[ -n "$config_image" ]] || return 0
    # Drop a pinned digest before taking the tag, or the digest would pass for the tag
    tag="${config_image%%@*}"
    tag="${tag##*:}"
    image_id=$(inspect_field "$container" '{{ .Image }}')
    digest=$(docker image inspect --format '{{ range .RepoDigests }}{{ println . }}{{ end }}' "$image_id" 2>/dev/null \
        | grep "^${DOCKER_REGISTRY}/${repo}@" | head -1 | sed 's/.*@//' || true)
    if [[ -n "$digest" ]]; then
        echo "${tag}@${digest}"
    else
        echo "$tag"
    fi
}

CURRENT_API_REF=$(current_ref "$API_CONTAINER" "$API_IMAGE")
CURRENT_UI_REF=""
if [[ -n "$UI_CONTAINER" ]]; then
    CURRENT_UI_REF=$(current_ref "$UI_CONTAINER" "$UI_IMAGE")
fi

DETECTED_API_PROJECT=$(inspect_field "$API_CONTAINER" '{{ index .Config.Labels "com.docker.compose.project" }}')
DETECTED_UI_PROJECT=""
if [[ -n "$UI_CONTAINER" ]]; then
    DETECTED_UI_PROJECT=$(inspect_field "$UI_CONTAINER" '{{ index .Config.Labels "com.docker.compose.project" }}')
fi

# A dev container started by the old dev-run.sh (docker run, no compose label) is replaced
ADOPT_API_CONTAINER=0
if is_dev; then
    COMPOSE_PROJECT="$COMPOSE_PROJECT_DEFAULT"
    if [[ -n "$CURRENT_API_REF" && "$DETECTED_API_PROJECT" != "$COMPOSE_PROJECT" ]]; then
        [[ -z "$DETECTED_API_PROJECT" ]] \
            || die "$API_CONTAINER belongs to compose project '$DETECTED_API_PROJECT', not '$COMPOSE_PROJECT'. Manual investigation required."
        ADOPT_API_CONTAINER=1
    fi
else
    # Project name: env override > script default
    COMPOSE_PROJECT="${COMPOSE_PROJECT_NAME:-$COMPOSE_PROJECT_DEFAULT}"

    # Both containers, if running, must agree on the project label
    if [[ -n "$DETECTED_API_PROJECT" && -n "$DETECTED_UI_PROJECT" \
          && "$DETECTED_API_PROJECT" != "$DETECTED_UI_PROJECT" ]]; then
        die "$API_CONTAINER compose project '$DETECTED_API_PROJECT' != $UI_CONTAINER '$DETECTED_UI_PROJECT'. Manual investigation required."
    fi
    DETECTED_PROJECT="${DETECTED_API_PROJECT:-$DETECTED_UI_PROJECT}"
    if [[ -n "$DETECTED_PROJECT" && "$DETECTED_PROJECT" != "$COMPOSE_PROJECT" ]]; then
        die "Detected compose project '$DETECTED_PROJECT' does not match script default '$COMPOSE_PROJECT'.
       Re-run with: COMPOSE_PROJECT_NAME='$DETECTED_PROJECT' $0 $DEPLOY_ENV ..."
    fi
fi

# --- Step 3: Resolve effective refs ---
EFFECTIVE_API_REF="${ARG_API_REF:-$CURRENT_API_REF}"
EFFECTIVE_UI_REF="${ARG_UI_REF:-$CURRENT_UI_REF}"

if [[ -z "$EFFECTIVE_API_REF" ]]; then
    die "No ref for api: container not running and --api not supplied."
fi
if [[ -n "$UI_CONTAINER" && -z "$EFFECTIVE_UI_REF" ]]; then
    die "No ref for ui: container not running and --ui not supplied."
fi

CHANGED_SERVICES=()
[[ -n "$ARG_API_REF" ]] && CHANGED_SERVICES+=("backend")
[[ -n "$ARG_UI_REF" ]] && CHANGED_SERVICES+=("frontend")

compose() {
    local file="$1"
    local api_ref="$2"
    local ui_ref="$3"
    shift 3
    API_TAG="$api_ref" UI_TAG="$ui_ref" docker compose -p "$COMPOSE_PROJECT" -f "$file" "$@"
}

# --- Step 4: Diff + confirm ---
echo ""
echo "=================================================="
echo " ${PLAN_TITLE}"
echo "=================================================="
echo " Host            : $(hostname)"
echo " Compose project : $COMPOSE_PROJECT"
echo " Compose file    : $COMPOSE_FILE"
echo ""
printf " %-10s %-30s %s\n" "Service" "Current" "Target"
printf " %-10s %-30s %s\n" "-------" "-------" "------"
if [[ -n "$ARG_API_REF" ]]; then
    printf " %-10s %-30s %s\n" "api" "${CURRENT_API_REF:-<none>}" "-> $ARG_API_REF"
else
    printf " %-10s %-30s %s\n" "api" "${CURRENT_API_REF:-<none>}" "(unchanged)"
fi
if [[ -n "$UI_CONTAINER" ]]; then
    if [[ -n "$ARG_UI_REF" ]]; then
        printf " %-10s %-30s %s\n" "ui" "${CURRENT_UI_REF:-<none>}" "-> $ARG_UI_REF"
    else
        printf " %-10s %-30s %s\n" "ui" "${CURRENT_UI_REF:-<none>}" "(unchanged)"
    fi
fi
echo "=================================================="

if [[ $DRY_RUN -eq 1 ]]; then
    echo ""
    echo "--- Rendered compose config (${CHANGED_SERVICES[*]}) ---"
    if is_dev && [[ ! -f "$DEV_TOKEN_ENV" ]]; then
        # compose won't load the file without its env_file, which a real run creates first
        echo "  (skipped: ${DEV_TOKEN_ENV} doesn't exist yet)"
    else
        compose "$COMPOSE_FILE" "$EFFECTIVE_API_REF" "$EFFECTIVE_UI_REF" config "${CHANGED_SERVICES[@]}"
    fi
    echo ""
    echo "--- Would run ---"
    if is_dev; then
        echo "  docker volume create ... $VOLUME_NAME   (only if missing)"
        echo "  create ${DEV_TOKEN_ENV}   (only if missing: copied from ${LEGACY_DEV_TOKEN_ENV}, or a new random secret)"
        echo "  set COMICS_DEVTOKEN_DEFAULTUSERNAME=${DEV_TOKEN_DEFAULT_USERNAME} in it   (only if missing)"
        [[ $ADOPT_API_CONTAINER -eq 1 ]] && echo "  docker stop $API_CONTAINER && docker rm $API_CONTAINER   (not created by compose)"
    fi
    echo "  API_TAG='$EFFECTIVE_API_REF' UI_TAG='$EFFECTIVE_UI_REF' docker compose -p $COMPOSE_PROJECT -f $COMPOSE_FILE pull ${CHANGED_SERVICES[*]}"
    echo "  API_TAG='$EFFECTIVE_API_REF' UI_TAG='$EFFECTIVE_UI_REF' docker compose -p $COMPOSE_PROJECT -f $COMPOSE_FILE up -d --no-deps ${CHANGED_SERVICES[*]}"
    if is_dev; then
        echo "  no rollback (dev)"
    else
        echo "  rollback baseline: api=${CURRENT_API_REF:-<none>} ui=${CURRENT_UI_REF:-<none>}"
    fi
    echo ""
    echo "Dry run: nothing changed."
    exit 0
fi

if ! is_dev; then
    echo ""
    if ! read -r -p "Continue? [y/N] " REPLY; then
        echo ""
        echo "Aborted: no answer (stdin closed). Run it from a terminal, or through utils/deploy.sh, which uses ssh -t."
        exit 1
    fi
    if [[ ! "$REPLY" =~ ^[yY]$ ]]; then
        echo "Aborted."
        exit 1
    fi

    # --- Step 5: Audit log entry ---
    TS=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    if [[ ! -f "$AUDIT_LOG" ]]; then
        touch "$AUDIT_LOG"
        chmod 600 "$AUDIT_LOG"
    fi
    echo "$TS user=${SUDO_USER:-$USER} args=api:${ARG_API_REF:-skip},ui:${ARG_UI_REF:-skip} from=api:${CURRENT_API_REF:-none},ui:${CURRENT_UI_REF:-none} to=api:${EFFECTIVE_API_REF},ui:${EFFECTIVE_UI_REF}" >> "$AUDIT_LOG"
fi

# --- Step 6: Compose pull + up ---
# Dev only, before the pull: the NFS volume and the devToken settings the compose file needs.
# Neither touches the running container
dev_prepare_files() {
    if ! docker volume ls -q | grep -q "^${VOLUME_NAME}$"; then
        echo "Creating NFS volume ${VOLUME_NAME}..."
        docker volume create \
            --driver local \
            --opt type=nfs4 \
            --opt "o=addr=10.0.0.48,rsize=1048576,wsize=1048576,timeo=600,retrans=2,noresvport,rw,noatime,nconnect=16,vers=4.1" \
            --opt "device=:/volume1/PodGeneral/comics-dev" \
            "$VOLUME_NAME"
    fi

    # Generated once, then reused so the secret stays stable. Kept on the Docker host, never in git
    if [[ ! -f "$DEV_TOKEN_ENV" ]]; then
        if [[ -f "$LEGACY_DEV_TOKEN_ENV" ]]; then
            echo "Copying ${LEGACY_DEV_TOKEN_ENV} to ${DEV_TOKEN_ENV}..."
            cp -p "$LEGACY_DEV_TOKEN_ENV" "$DEV_TOKEN_ENV"
            chmod 600 "$DEV_TOKEN_ENV"
        else
            echo "Creating ${DEV_TOKEN_ENV} with a new devToken secret..."
            (
                umask 077
                printf 'COMICS_DEVTOKEN_ENABLED=true\nCOMICS_DEVTOKEN_SECRET=%s\n' "$(openssl rand -hex 32)" > "$DEV_TOKEN_ENV"
            )
        fi
    fi
    if ! grep -q '^COMICS_DEVTOKEN_DEFAULTUSERNAME=' "$DEV_TOKEN_ENV"; then
        echo "Setting the default devToken username to ${DEV_TOKEN_DEFAULT_USERNAME} in ${DEV_TOKEN_ENV}..."
        echo "COMICS_DEVTOKEN_DEFAULTUSERNAME=${DEV_TOKEN_DEFAULT_USERNAME}" >> "$DEV_TOKEN_ENV"
    fi
}

# Dev only, after the pull succeeds: compose can't take over a container it didn't create
dev_remove_legacy_container() {
    if [[ $ADOPT_API_CONTAINER -eq 1 ]]; then
        echo "Removing ${API_CONTAINER}, which compose didn't create..."
        docker stop "$API_CONTAINER" >/dev/null
        docker rm "$API_CONTAINER" >/dev/null
    fi
}

# pull runs first, so a ref missing from the registry fails before anything is recreated.
compose_pull() {
    local file="$1"
    shift
    compose "$file" "$@" pull "${CHANGED_SERVICES[@]}"
}

compose_up() {
    local file="$1"
    shift
    compose "$file" "$@" up -d --no-deps "${CHANGED_SERVICES[@]}"
}

# --- Step 7: Health poll ---
container_for_service() {
    case "$1" in
        backend) echo "$API_CONTAINER" ;;
        frontend) echo "$UI_CONTAINER" ;;
        *) die "Unknown service: $1" ;;
    esac
}

wait_healthy() {
    local services=("$@")
    local elapsed=0
    while (( elapsed < HEALTH_TIMEOUT_SECS )); do
        local all_healthy=1
        for svc in "${services[@]}"; do
            local container
            container=$(container_for_service "$svc")
            local status
            status=$(docker inspect \
                --format '{{ if .State.Health }}{{ .State.Health.Status }}{{ else }}no-healthcheck{{ end }}' \
                "$container" 2>/dev/null || echo "missing")
            printf "  [t=%3ds] %-15s %s\n" "$elapsed" "$container" "$status"
            if [[ "$status" != "healthy" ]]; then
                all_healthy=0
            fi
        done
        if (( all_healthy == 1 )); then
            return 0
        fi
        sleep "$HEALTH_POLL_INTERVAL"
        elapsed=$(( elapsed + HEALTH_POLL_INTERVAL ))
    done
    return 1
}

echo ""
echo "--- Deploying ${CHANGED_SERVICES[*]} via compose ---"
if is_dev; then
    dev_prepare_files
fi
if ! compose_pull "$COMPOSE_FILE" "$EFFECTIVE_API_REF" "$EFFECTIVE_UI_REF"; then
    echo ""
    echo "Pull failed: no container was changed."
    exit 1
fi
if is_dev; then
    dev_remove_legacy_container
fi

DEPLOYED=1
if ! compose_up "$COMPOSE_FILE" "$EFFECTIVE_API_REF" "$EFFECTIVE_UI_REF"; then
    DEPLOYED=0
    echo ""
    echo "compose up failed."
fi

if (( DEPLOYED == 1 )); then
    echo ""
    echo "--- Polling health (timeout ${HEALTH_TIMEOUT_SECS}s) ---"
    if wait_healthy "${CHANGED_SERVICES[@]}"; then
        # The compose file a rollback returns to
        cp "$COMPOSE_FILE" "$LAST_GOOD_COMPOSE_FILE"
        echo ""
        if is_dev; then
            echo "Dev deploy successful."
            echo "   api: ${API_CONTAINER} @ ${EFFECTIVE_API_REF} (port ${API_PORT})"
            echo ""
            echo "   Tail logs: docker logs -f $API_CONTAINER"
        else
            echo "Deploy successful."
            echo "   api: ${API_CONTAINER} @ ${EFFECTIVE_API_REF} (port ${API_PORT})"
            echo "   ui:  ${UI_CONTAINER} @ ${EFFECTIVE_UI_REF}  (port ${UI_PORT})"
            echo ""
            echo "   Tail logs: docker logs -f $API_CONTAINER"
            echo "              docker logs -f $UI_CONTAINER"
            echo "   Audit log: $AUDIT_LOG"
        fi
        exit 0
    fi
fi

if is_dev; then
    echo ""
    echo "${API_CONTAINER} is not healthy; it is left running (no rollback on dev)."
    echo "Startup catch-up jobs can hold readiness down for a while."
    echo "  Logs: docker logs --tail 100 ${API_CONTAINER}"
    exit 1
fi

# --- Step 8: Auto-rollback ---
ROLLBACK_COMPOSE_FILE="$COMPOSE_FILE"
[[ -f "$LAST_GOOD_COMPOSE_FILE" ]] && ROLLBACK_COMPOSE_FILE="$LAST_GOOD_COMPOSE_FILE"

echo ""
echo "Deploy failed. Rolling back..."
echo "   api: ${EFFECTIVE_API_REF} -> ${CURRENT_API_REF}"
echo "   ui:  ${EFFECTIVE_UI_REF} -> ${CURRENT_UI_REF}"
echo "   compose file: ${ROLLBACK_COMPOSE_FILE}"

if [[ -z "$CURRENT_API_REF" || -z "$CURRENT_UI_REF" ]]; then
    echo ""
    echo "MANUAL INTERVENTION REQUIRED: could not capture rollback baseline."
    echo "$TS ROLLBACK_FAILED reason=no-baseline" >> "$AUDIT_LOG"
    exit 2
fi

if compose_pull "$ROLLBACK_COMPOSE_FILE" "$CURRENT_API_REF" "$CURRENT_UI_REF" \
        && compose_up "$ROLLBACK_COMPOSE_FILE" "$CURRENT_API_REF" "$CURRENT_UI_REF" \
        && wait_healthy "${CHANGED_SERVICES[@]}"; then
    echo ""
    echo "Rollback successful. Previous images restored."
    echo "$TS ROLLBACK_OK from=api:${EFFECTIVE_API_REF},ui:${EFFECTIVE_UI_REF} to=api:${CURRENT_API_REF},ui:${CURRENT_UI_REF}" >> "$AUDIT_LOG"
    exit 1
fi

echo ""
echo "MANUAL INTERVENTION REQUIRED: rollback also failed health check."
echo "$TS ROLLBACK_FAILED reason=unhealthy-after-rollback" >> "$AUDIT_LOG"
exit 2
