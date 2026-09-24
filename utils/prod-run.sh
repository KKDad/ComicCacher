#!/bin/bash
#
# Deploy comic-api and/or comic-ui on the PRODUCTION Docker host (Portainer).
#
# Runs ON the Docker host with its local `docker`, using the docker-compose.yml
# that sits next to this script. The workstation stages both files here with
# prod-build-and-run.sh (or by hand); nothing in this script builds images.
#
# Steps:
#   - Capture the running image of each container, by digest, as the rollback baseline
#   - Compose project label on running containers must match COMPOSE_PROJECT
#   - Operator confirmation prompt with current -> target diff
#   - Audit log appended to ~/.comiccacher-prod-deploy.log on this host
#   - compose pull + up -d --no-deps for the changed services only
#   - Polls Docker health status; on failure, rolls back to the baseline digests
#
# Usage:
#   ./prod-run.sh --api 2.4.6
#   ./prod-run.sh --api 2.4.6@sha256:<64 hex>        # pin the exact image
#   ./prod-run.sh --api 2.4.6 --ui 2.4.1 --dry-run     # show the plan, change nothing
#

set -euo pipefail

# --- Constants ---
DOCKER_REGISTRY="registry.stapledon.ca"
API_IMAGE="kkdad/comic-api"
UI_IMAGE="kkdad/comic-ui"
API_CONTAINER="comics-api"
UI_CONTAINER="comics-ui"
COMPOSE_PROJECT_DEFAULT="comics"
HEALTH_TIMEOUT_SECS=180
HEALTH_POLL_INTERVAL=3
# semver, optionally pinned by digest: 2.4.6, 2.4.6-rc1, 2.4.6@sha256:<64 hex>
REF_REGEX='^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.]+)?(@sha256:[0-9a-f]{64})?$'
AUDIT_LOG="${HOME}/.comiccacher-prod-deploy.log"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.yml"

usage() {
    cat <<EOF
Usage: $0 [--api <ref>] [--ui <ref>] [--dry-run]

Run on the production Docker host. At least one of --api / --ui is required.
<ref> is a semver tag (2.4.6) or a tag pinned by digest (2.4.6@sha256:...).

  --api <ref>   Deploy comic-api at <ref>
  --ui <ref>    Deploy comic-ui at <ref>
  --dry-run     Print the plan, the rendered compose config and the docker
                commands, then exit without changing anything
  -h, --help    Show this help

Compose file: ${COMPOSE_FILE}

Environment overrides:
  COMPOSE_PROJECT_NAME   Override compose project name (default: ${COMPOSE_PROJECT_DEFAULT})
EOF
}

die() {
    echo "Error: $*" >&2
    exit 1
}

# --- Step 1: Arg parse + ref validation ---
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

for ref in "$ARG_API_REF" "$ARG_UI_REF"; do
    if [[ -n "$ref" && ! "$ref" =~ $REF_REGEX ]]; then
        die "'$ref' is not a valid ref (expected e.g. 2.4.6, 2.4.6-rc1 or 2.4.6@sha256:<64 hex>)."
    fi
done

[[ -f "$COMPOSE_FILE" ]] || die "Compose file not found: $COMPOSE_FILE"

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
    tag="${config_image##*:}"
    tag="${tag%%@*}"
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
CURRENT_UI_REF=$(current_ref "$UI_CONTAINER" "$UI_IMAGE")

DETECTED_API_PROJECT=$(inspect_field "$API_CONTAINER" '{{ index .Config.Labels "com.docker.compose.project" }}')
DETECTED_UI_PROJECT=$(inspect_field "$UI_CONTAINER" '{{ index .Config.Labels "com.docker.compose.project" }}')

# Project name: env override > script default
COMPOSE_PROJECT="${COMPOSE_PROJECT_NAME:-$COMPOSE_PROJECT_DEFAULT}"

# Both containers, if running, must agree on the project label
if [[ -n "$DETECTED_API_PROJECT" && -n "$DETECTED_UI_PROJECT" \
      && "$DETECTED_API_PROJECT" != "$DETECTED_UI_PROJECT" ]]; then
    die "comics-api compose project '$DETECTED_API_PROJECT' != comics-ui '$DETECTED_UI_PROJECT'. Manual investigation required."
fi
DETECTED_PROJECT="${DETECTED_API_PROJECT:-$DETECTED_UI_PROJECT}"
if [[ -n "$DETECTED_PROJECT" && "$DETECTED_PROJECT" != "$COMPOSE_PROJECT" ]]; then
    die "Detected compose project '$DETECTED_PROJECT' does not match script default '$COMPOSE_PROJECT'.
       Re-run with: COMPOSE_PROJECT_NAME='$DETECTED_PROJECT' $0 $*"
fi

# --- Step 3: Resolve effective refs ---
EFFECTIVE_API_REF="${ARG_API_REF:-$CURRENT_API_REF}"
EFFECTIVE_UI_REF="${ARG_UI_REF:-$CURRENT_UI_REF}"

if [[ -z "$EFFECTIVE_API_REF" ]]; then
    die "No ref for api: container not running and --api not supplied."
fi
if [[ -z "$EFFECTIVE_UI_REF" ]]; then
    die "No ref for ui: container not running and --ui not supplied."
fi

CHANGED_SERVICES=()
[[ -n "$ARG_API_REF" ]] && CHANGED_SERVICES+=("backend")
[[ -n "$ARG_UI_REF" ]] && CHANGED_SERVICES+=("frontend")

compose() {
    local api_ref="$1"
    local ui_ref="$2"
    shift 2
    API_TAG="$api_ref" UI_TAG="$ui_ref" docker compose -p "$COMPOSE_PROJECT" -f "$COMPOSE_FILE" "$@"
}

# --- Step 4: Diff + confirm ---
echo ""
echo "=================================================="
echo " Production Deploy Plan"
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
if [[ -n "$ARG_UI_REF" ]]; then
    printf " %-10s %-30s %s\n" "ui" "${CURRENT_UI_REF:-<none>}" "-> $ARG_UI_REF"
else
    printf " %-10s %-30s %s\n" "ui" "${CURRENT_UI_REF:-<none>}" "(unchanged)"
fi
echo "=================================================="

if [[ $DRY_RUN -eq 1 ]]; then
    echo ""
    echo "--- Rendered compose config (${CHANGED_SERVICES[*]}) ---"
    compose "$EFFECTIVE_API_REF" "$EFFECTIVE_UI_REF" config "${CHANGED_SERVICES[@]}"
    echo ""
    echo "--- Would run ---"
    echo "  API_TAG='$EFFECTIVE_API_REF' UI_TAG='$EFFECTIVE_UI_REF' docker compose -p $COMPOSE_PROJECT -f $COMPOSE_FILE pull ${CHANGED_SERVICES[*]}"
    echo "  API_TAG='$EFFECTIVE_API_REF' UI_TAG='$EFFECTIVE_UI_REF' docker compose -p $COMPOSE_PROJECT -f $COMPOSE_FILE up -d --no-deps ${CHANGED_SERVICES[*]}"
    echo "  rollback baseline: api=${CURRENT_API_REF:-<none>} ui=${CURRENT_UI_REF:-<none>}"
    echo ""
    echo "Dry run: nothing changed."
    exit 0
fi

echo ""
read -r -p "Continue? [y/N] " REPLY
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

# --- Step 6: Compose pull + up ---
# pull runs first, so a ref missing from the registry fails before anything is recreated.
compose_apply() {
    local api_ref="$1"
    local ui_ref="$2"
    shift 2
    compose "$api_ref" "$ui_ref" pull "$@"
    compose "$api_ref" "$ui_ref" up -d --no-deps "$@"
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
compose_apply "$EFFECTIVE_API_REF" "$EFFECTIVE_UI_REF" "${CHANGED_SERVICES[@]}"

echo ""
echo "--- Polling health (timeout ${HEALTH_TIMEOUT_SECS}s) ---"
if wait_healthy "${CHANGED_SERVICES[@]}"; then
    echo ""
    echo "Deploy successful."
    echo "   api: ${API_CONTAINER} @ ${EFFECTIVE_API_REF} (port 8888)"
    echo "   ui:  ${UI_CONTAINER} @ ${EFFECTIVE_UI_REF}  (port 8899)"
    echo ""
    echo "   Tail logs: docker logs -f $API_CONTAINER"
    echo "              docker logs -f $UI_CONTAINER"
    echo "   Audit log: $AUDIT_LOG"
    exit 0
fi

# --- Step 8: Auto-rollback ---
echo ""
echo "Health check failed. Rolling back..."
echo "   api: ${EFFECTIVE_API_REF} -> ${CURRENT_API_REF}"
echo "   ui:  ${EFFECTIVE_UI_REF} -> ${CURRENT_UI_REF}"

if [[ -z "$CURRENT_API_REF" || -z "$CURRENT_UI_REF" ]]; then
    echo ""
    echo "MANUAL INTERVENTION REQUIRED: could not capture rollback baseline."
    echo "$TS ROLLBACK_FAILED reason=no-baseline" >> "$AUDIT_LOG"
    exit 2
fi

if compose_apply "$CURRENT_API_REF" "$CURRENT_UI_REF" "${CHANGED_SERVICES[@]}" \
        && wait_healthy "${CHANGED_SERVICES[@]}"; then
    echo ""
    echo "Rollback successful. Previous images restored."
    echo "   Note: the rollback reuses this compose file; restore the previous compose file too if it changed."
    echo "$TS ROLLBACK_OK from=api:${EFFECTIVE_API_REF},ui:${EFFECTIVE_UI_REF} to=api:${CURRENT_API_REF},ui:${CURRENT_UI_REF}" >> "$AUDIT_LOG"
    exit 1
fi

echo ""
echo "MANUAL INTERVENTION REQUIRED: rollback also failed health check."
echo "$TS ROLLBACK_FAILED reason=unhealthy-after-rollback" >> "$AUDIT_LOG"
exit 2
