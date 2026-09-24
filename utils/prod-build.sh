#!/bin/bash
#
# Build and push comic-api and/or comic-ui images for PRODUCTION.
#
# Runs on a workstation (Ubuntu with Podman, or macOS with Docker Desktop).
# Builds only; deploying is prod-run.sh on the Docker host. Kept compatible
# with macOS bash 3.2 and BSD tools, and never uses a Docker context.
#
# Pre-flight gates:
#   - Git: must be on master with a clean working tree
#   - Semver tag validation
# Post-build:
#   - Image must exist in registry
#
# Usage:
#   ./utils/prod-build.sh --api 2.4.6
#   ./utils/prod-build.sh --api 2.4.6 --ui 2.4.1
#

set -euo pipefail

DOCKER_REGISTRY="registry.stapledon.ca"
API_IMAGE="kkdad/comic-api"
UI_IMAGE="kkdad/comic-ui"
SEMVER_REGEX='^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.]+)?$'

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

usage() {
    cat <<EOF
Usage: $0 [--api <version>] [--ui <version>]

At least one of --api / --ui is required.

  --api <version>   Build and push comic-api at <version>
  --ui <version>    Build and push comic-ui at <version>
  -h, --help        Show this help
EOF
}

die() {
    echo "Error: $*" >&2
    exit 1
}

ARG_API_TAG=""
ARG_UI_TAG=""

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

# --- Git gate ---
BRANCH=$(git -C "$PROJECT_ROOT" rev-parse --abbrev-ref HEAD)
if [[ "$BRANCH" != "master" ]]; then
    die "Must be on 'master' branch (currently '$BRANCH')."
fi
if [[ -n "$(git -C "$PROJECT_ROOT" status --porcelain)" ]]; then
    die "Working tree is dirty. Commit or stash before building."
fi
echo "Building from master @ $(git -C "$PROJECT_ROOT" rev-parse --short HEAD)"

# --- Build + push ---
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

# --- Verify image exists in registry ---
verify_in_registry() {
    local image="$1"
    local tag="$2"
    local url="https://${DOCKER_REGISTRY}/v2/${image}/manifests/${tag}"
    if ! curl -fsSI \
        -H "Accept: application/vnd.oci.image.manifest.v1+json,application/vnd.docker.distribution.manifest.v2+json,application/vnd.oci.image.index.v1+json,application/vnd.docker.distribution.manifest.list.v2+json" \
        "$url" >/dev/null; then
        die "Image ${image}:${tag} not found in registry (${url})"
    fi
}

echo ""
echo "--- Verifying images in registry ---"
if [[ -n "$ARG_API_TAG" ]]; then
    verify_in_registry "$API_IMAGE" "$ARG_API_TAG"
    echo "  ✓ ${API_IMAGE}:${ARG_API_TAG}"
fi
if [[ -n "$ARG_UI_TAG" ]]; then
    verify_in_registry "$UI_IMAGE" "$ARG_UI_TAG"
    echo "  ✓ ${UI_IMAGE}:${ARG_UI_TAG}"
fi
