#!/bin/bash
#
# Build, push, and deploy comic-api and/or comic-ui to PRODUCTION.
#
# Pre-flight gates:
#   - Git: must be on master with a clean working tree
#   - Semver tag validation
#   - Image must exist in registry (post-build, pre-deploy)
#   - Compose project label on running containers must match COMPOSE_PROJECT
#   - Operator confirmation prompt with current → target diff
#
# Post-deploy:
#   - Polls Docker health status (HEALTHCHECK already defined in both Dockerfiles)
#   - On health failure, auto-rolls back to the previously running tags
#   - Audit log appended to ~/.comiccacher-prod-deploy.log
#
# Usage:
#   ./utils/prod-build-and-run.sh --api 2.4.6
#   ./utils/prod-build-and-run.sh --api 2.4.6 --ui 2.4.1
#   ./utils/prod-build-and-run.sh --ui 2.4.1 --skip-build      # use already-pushed image
#   COMPOSE_PROJECT_NAME=mystack ./utils/prod-build-and-run.sh --api 2.4.6
#

set -euo pipefail

# --- Constants ---
DOCKER_CONTEXT="portainer"
DOCKER_REGISTRY="registry.stapledon.ca"
API_IMAGE="kkdad/comic-api"
UI_IMAGE="kkdad/comic-ui"
API_CONTAINER="comics-api"
UI_CONTAINER="comics-ui"
COMPOSE_PROJECT_DEFAULT="comics"
HEALTH_TIMEOUT_SECS=180
HEALTH_POLL_INTERVAL=3
SEMVER_REGEX='^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.]+)?$'
AUDIT_LOG="${HOME}/.comiccacher-prod-deploy.log"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${PROJECT_ROOT}/utils/prod/docker-compose.yml"

usage() {
    cat <<EOF
Usage: $0 [--api <version>] [--ui <version>] [--skip-build]

At least one of --api / --ui is required.

  --api <version>   Build, push, and deploy comic-api at <version>
  --ui <version>    Build, push, and deploy comic-ui at <version>
  --skip-build      Skip gradle/docker build + push; image must already be in registry
  -h, --help        Show this help

Environment overrides:
  COMPOSE_PROJECT_NAME   Override compose project name (default: ${COMPOSE_PROJECT_DEFAULT})
EOF
}

die() {
    echo "Error: $*" >&2
    exit 1
}

# --- Step 1: Arg parse + semver validation ---
ARG_API_TAG=""
ARG_UI_TAG=""
SKIP_BUILD=0

while [[ $# -gt 0 ]]; do
    case "$1" in
        --api)
            [[ $# -ge 2 ]] || die "--api requires a version argument"
            ARG_API_TAG="$2"
            shift 2
            ;;
        --ui)
            [[ $# -ge 2 ]] || die "--ui requires a version argument"
            ARG_UI_TAG="$2"
            shift 2
            ;;
        --skip-build)
            SKIP_BUILD=1
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

if [[ -z "$ARG_API_TAG" && -z "$ARG_UI_TAG" ]]; then
    usage
    die "At least one of --api or --ui is required."
fi

for tag in "$ARG_API_TAG" "$ARG_UI_TAG"; do
    if [[ -n "$tag" && ! "$tag" =~ $SEMVER_REGEX ]]; then
        die "'$tag' is not a valid semver tag (expected e.g. 2.4.6 or 2.4.6-rc1)."
    fi
done

# --- Step 2: Git gate ---
BRANCH=$(git -C "$PROJECT_ROOT" rev-parse --abbrev-ref HEAD)
if [[ "$BRANCH" != "master" ]]; then
    die "Must be on 'master' branch (currently '$BRANCH')."
fi
if [[ -n "$(git -C "$PROJECT_ROOT" status --porcelain)" ]]; then
    die "Working tree is dirty. Commit or stash before deploying."
fi
GIT_SHA=$(git -C "$PROJECT_ROOT" rev-parse --short HEAD)

# --- Step 3: Capture current state from running containers ---
inspect_field() {
    local container="$1"
    local format="$2"
    docker --context "$DOCKER_CONTEXT" inspect --format "$format" "$container" 2>/dev/null || true
}

CURRENT_API_IMAGE=$(inspect_field "$API_CONTAINER" '{{ .Config.Image }}')
CURRENT_UI_IMAGE=$(inspect_field "$UI_CONTAINER" '{{ .Config.Image }}')
CURRENT_API_TAG=""
CURRENT_UI_TAG=""
[[ -n "$CURRENT_API_IMAGE" ]] && CURRENT_API_TAG="${CURRENT_API_IMAGE##*:}"
[[ -n "$CURRENT_UI_IMAGE" ]] && CURRENT_UI_TAG="${CURRENT_UI_IMAGE##*:}"

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

# --- Step 4: Resolve effective tags ---
EFFECTIVE_API_TAG="${ARG_API_TAG:-$CURRENT_API_TAG}"
EFFECTIVE_UI_TAG="${ARG_UI_TAG:-$CURRENT_UI_TAG}"

if [[ -z "$EFFECTIVE_API_TAG" ]]; then
    die "No tag for api: container not running and --api not supplied."
fi
if [[ -z "$EFFECTIVE_UI_TAG" ]]; then
    die "No tag for ui: container not running and --ui not supplied."
fi

# --- Step 5: Diff + confirm ---
echo ""
echo "=================================================="
echo " Production Deploy Plan"
echo "=================================================="
echo " Compose project : $COMPOSE_PROJECT"
echo " Docker context  : $DOCKER_CONTEXT"
echo " Git SHA         : $GIT_SHA"
echo " Skip build      : $([[ $SKIP_BUILD -eq 1 ]] && echo yes || echo no)"
echo ""
printf " %-10s %-15s %s\n" "Service" "Current" "Target"
printf " %-10s %-15s %s\n" "-------" "-------" "------"
if [[ -n "$ARG_API_TAG" ]]; then
    printf " %-10s %-15s %s\n" "api" "${CURRENT_API_TAG:-<none>}" "→ $ARG_API_TAG"
else
    printf " %-10s %-15s %s\n" "api" "${CURRENT_API_TAG:-<none>}" "(unchanged)"
fi
if [[ -n "$ARG_UI_TAG" ]]; then
    printf " %-10s %-15s %s\n" "ui" "${CURRENT_UI_TAG:-<none>}" "→ $ARG_UI_TAG"
else
    printf " %-10s %-15s %s\n" "ui" "${CURRENT_UI_TAG:-<none>}" "(unchanged)"
fi
echo "=================================================="
echo ""
read -r -p "Continue? [y/N] " REPLY
if [[ ! "$REPLY" =~ ^[yY]$ ]]; then
    echo "Aborted."
    exit 1
fi

# --- Step 6: Build + push ---
if [[ $SKIP_BUILD -eq 0 ]]; then
    if [[ -n "$ARG_API_TAG" ]]; then
        echo ""
        echo "--- Building + pushing comic-api $ARG_API_TAG ---"
        "$PROJECT_ROOT/comic-api/build-docker.sh" "$ARG_API_TAG"
    fi
    if [[ -n "$ARG_UI_TAG" ]]; then
        echo ""
        echo "--- Building + pushing comic-ui $ARG_UI_TAG ---"
        "$PROJECT_ROOT/comic-hub/build-docker.sh" "$ARG_UI_TAG"
    fi
fi

# --- Step 7: Verify image exists in registry ---
verify_in_registry() {
    local image="$1"
    local tag="$2"
    local url="https://${DOCKER_REGISTRY}/v2/${image}/manifests/${tag}"
    if ! curl -fsSI \
        -H "Accept: application/vnd.oci.image.manifest.v1+json,application/vnd.docker.distribution.manifest.v2+json" \
        "$url" >/dev/null; then
        die "Image ${image}:${tag} not found in registry (${url})"
    fi
}

echo ""
echo "--- Verifying images in registry ---"
verify_in_registry "$API_IMAGE" "$EFFECTIVE_API_TAG"
echo "  ✓ ${API_IMAGE}:${EFFECTIVE_API_TAG}"
verify_in_registry "$UI_IMAGE" "$EFFECTIVE_UI_TAG"
echo "  ✓ ${UI_IMAGE}:${EFFECTIVE_UI_TAG}"

# --- Step 8: Audit log entry ---
TS=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
if [[ ! -f "$AUDIT_LOG" ]]; then
    touch "$AUDIT_LOG"
    chmod 600 "$AUDIT_LOG"
fi
echo "$TS user=$USER sha=$GIT_SHA args=api:${ARG_API_TAG:-skip},ui:${ARG_UI_TAG:-skip} from=api:${CURRENT_API_TAG:-none},ui:${CURRENT_UI_TAG:-none} to=api:${EFFECTIVE_API_TAG},ui:${EFFECTIVE_UI_TAG}" >> "$AUDIT_LOG"

# --- Step 9-10: Compose pull + up ---
CHANGED_SERVICES=()
[[ -n "$ARG_API_TAG" ]] && CHANGED_SERVICES+=("backend")
[[ -n "$ARG_UI_TAG" ]] && CHANGED_SERVICES+=("frontend")

compose_apply() {
    local api_tag="$1"
    local ui_tag="$2"
    shift 2
    local services=("$@")
    API_TAG="$api_tag" UI_TAG="$ui_tag" docker --context "$DOCKER_CONTEXT" compose \
        -p "$COMPOSE_PROJECT" -f "$COMPOSE_FILE" pull "${services[@]}"
    API_TAG="$api_tag" UI_TAG="$ui_tag" docker --context "$DOCKER_CONTEXT" compose \
        -p "$COMPOSE_PROJECT" -f "$COMPOSE_FILE" up -d --no-deps "${services[@]}"
}

# --- Step 11: Health poll ---
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
            status=$(docker --context "$DOCKER_CONTEXT" inspect \
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
compose_apply "$EFFECTIVE_API_TAG" "$EFFECTIVE_UI_TAG" "${CHANGED_SERVICES[@]}"

echo ""
echo "--- Polling health (timeout ${HEALTH_TIMEOUT_SECS}s) ---"
if wait_healthy "${CHANGED_SERVICES[@]}"; then
    echo ""
    echo "✅ Deploy successful."
    echo "   api: ${API_CONTAINER} @ ${EFFECTIVE_API_TAG} (port 8888)"
    echo "   ui:  ${UI_CONTAINER} @ ${EFFECTIVE_UI_TAG}  (port 8899)"
    echo ""
    echo "   Tail logs: docker --context $DOCKER_CONTEXT logs -f $API_CONTAINER"
    echo "              docker --context $DOCKER_CONTEXT logs -f $UI_CONTAINER"
    echo "   Audit log: $AUDIT_LOG"
    exit 0
fi

# --- Step 12: Auto-rollback ---
echo ""
echo "❌ Health check failed. Rolling back..."
echo "   api: ${EFFECTIVE_API_TAG} → ${CURRENT_API_TAG}"
echo "   ui:  ${EFFECTIVE_UI_TAG} → ${CURRENT_UI_TAG}"

if [[ -z "$CURRENT_API_TAG" || -z "$CURRENT_UI_TAG" ]]; then
    echo ""
    echo "🚨 MANUAL INTERVENTION REQUIRED — could not capture rollback baseline."
    echo "$TS ROLLBACK_FAILED reason=no-baseline" >> "$AUDIT_LOG"
    exit 2
fi

if compose_apply "$CURRENT_API_TAG" "$CURRENT_UI_TAG" "${CHANGED_SERVICES[@]}" \
        && wait_healthy "${CHANGED_SERVICES[@]}"; then
    echo ""
    echo "↩️  Rollback successful. Previous tags restored."
    echo "$TS ROLLBACK_OK from=api:${EFFECTIVE_API_TAG},ui:${EFFECTIVE_UI_TAG} to=api:${CURRENT_API_TAG},ui:${CURRENT_UI_TAG}" >> "$AUDIT_LOG"
    exit 1
fi

echo ""
echo "🚨 MANUAL INTERVENTION REQUIRED — rollback also failed health check."
echo "$TS ROLLBACK_FAILED reason=unhealthy-after-rollback" >> "$AUDIT_LOG"
exit 2
