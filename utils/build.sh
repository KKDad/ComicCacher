#!/bin/bash
#
# Build and push the comic-api and/or comic-ui images.
#
# Runs on a workstation (Ubuntu with Podman, or macOS with Docker Desktop). Builds only;
# deploy.sh runs it and then deploys. Kept compatible with macOS bash 3.2 and BSD tools,
# and never uses a Docker context.
#
#   prod: must be on master with a clean working tree
#   dev:  any branch, including uncommitted changes
#
# Both: semver tags; images are pushed with Skopeo straight to the registry's port 5000
# (bypassing Cloudflare's 100MB upload limit), then must be found in the registry.
#
# Usage:
#   ./utils/build.sh prod --api 2.4.6
#   ./utils/build.sh prod --api 2.4.6 --ui 2.4.1
#   ./utils/build.sh dev --api 2.4.8-rc1
#

set -euo pipefail

# shellcheck source-path=SCRIPTDIR source=lib/common.sh
. "$(cd "$(dirname "$0")" && pwd)/lib/common.sh"

usage() {
    cat <<EOF
Usage: $0 <dev|prod> [--api <version>] [--ui <version>]

At least one of --api / --ui is required. prod builds need master with a clean tree.

  --api <version>   Build and push comic-api at <version>
  --ui <version>    Build and push comic-ui at <version>
  -h, --help        Show this help
EOF
}

case "${1:-}" in
    -h|--help) usage; exit 0 ;;
esac
require_env "${1:-}"
shift

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

check_versions "$ARG_API_TAG" "$ARG_UI_TAG"

# --- Git gate ---
if is_dev; then
    echo "Building for dev from $(git -C "$PROJECT_ROOT" rev-parse --abbrev-ref HEAD) @ $(git -C "$PROJECT_ROOT" rev-parse --short HEAD)"
else
    require_clean_master "building"
    echo "Building from master @ $(git -C "$PROJECT_ROOT" rev-parse --short HEAD)"
fi

# --- Build + push ---
WORK_DIR=$(mktemp -d "${TMPDIR:-/tmp}/comic-build.XXXXXX")
trap 'rm -rf "$WORK_DIR"' EXIT

# Skopeo reads a saved tarball: its docker-daemon transport has issues with Docker Desktop
push_image() {
    local full_image="$1"
    local tarball
    tarball="${WORK_DIR}/$(basename "${full_image%:*}").tar"
    echo "--- Saving image to tarball ---"
    docker save "$full_image" -o "$tarball"
    echo "--- Pushing ${full_image} via Skopeo ---"
    skopeo copy \
        --dest-tls-verify=false \
        docker-archive:"$tarball" \
        docker://"$full_image"
    rm -f "$tarball"
    echo "--- Success! Image ${full_image} is now in the registry ---"
}

# Podman defaults to OCI format, which drops HEALTHCHECK; remote/run.sh needs it to report
# "healthy". Docker ignores BUILDAH_FORMAT.
build_api() {
    local tag="$1"
    local full_image="${PUSH_REGISTRY}/${API_IMAGE}:${tag}"
    echo ""
    echo "--- Building + pushing comic-api $tag ---"
    echo "--- Building bootJar with version ${tag} ---"
    (cd "$PROJECT_ROOT" && ./gradlew :comic-api:clean :comic-api:bootJar -PbuildVersion="$tag")
    echo "--- Building ${full_image} ---"
    (cd "$PROJECT_ROOT/comic-api" && BUILDAH_FORMAT=docker docker build -f Dockerfile . \
        --tag "$full_image" --build-arg VERSION="$tag" --platform linux/amd64)
    push_image "$full_image"
}

build_ui() {
    local tag="$1"
    local full_image="${PUSH_REGISTRY}/${UI_IMAGE}:${tag}"
    echo ""
    echo "--- Building + pushing comic-ui $tag ---"
    echo "--- Building ${full_image} ---"
    (cd "$PROJECT_ROOT/comic-hub" && BUILDAH_FORMAT=docker docker build -f Dockerfile . \
        --tag "$full_image" --platform linux/amd64)
    push_image "$full_image"
}

if [[ -n "$ARG_API_TAG" ]]; then
    build_api "$ARG_API_TAG"
fi
if [[ -n "$ARG_UI_TAG" ]]; then
    build_ui "$ARG_UI_TAG"
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
    echo "  ok: ${image}:${tag}"
}

echo ""
echo "--- Verifying images in registry ---"
if [[ -n "$ARG_API_TAG" ]]; then
    verify_in_registry "$API_IMAGE" "$ARG_API_TAG"
fi
if [[ -n "$ARG_UI_TAG" ]]; then
    verify_in_registry "$UI_IMAGE" "$ARG_UI_TAG"
fi
