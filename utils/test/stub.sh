#!/bin/bash
#
# Stand-in for docker, ssh, scp, skopeo, curl, git, gradlew, hostname, date, sleep and openssl
# in the utils tests. run-tests.sh links each name to this file. Nothing here reaches a real
# host, registry or repository: each call appends its argv to $TRACE and answers from the
# scenario's FX_* variables.
#
# Fixtures (all optional; a container with no FX_IMAGE_<name> doesn't exist):
#   FX_IMAGE_<container>          .Config.Image, e.g. registry.stapledon.ca/kkdad/comic-api:2.5.0
#   FX_DIGEST_<container>         RepoDigests entry of the running image
#   FX_PROJECT_<container>        com.docker.compose.project label
#   FX_HEALTH_<container>_<n>     health statuses after the n-th compose up / docker run
#                                 (0 = before any), space separated; the last one repeats
#   FX_FAIL                       space separated steps that fail: pull, up, up2, run, build, skopeo,
#                                 fetch, and missing-after-push (a pushed image isn't found)
#   FX_VOLUMES                    output of `docker volume ls -q`
#   FX_REGISTRY_INFO              space separated image:tag=digest|revision|dirty entries: what the
#                                 registry holds before the scenario pushes anything
#   FX_PUSHED_INFO                digest|revision|dirty that skopeo inspect reports for an image
#                                 the scenario pushed (default: digest aaa…, HEAD's revision, false)
#   FX_REGISTRY_STATUS            HTTP status the registry answers for a missing tag (default 404)
#   FX_ON_MASTER                  space separated revisions on origin/master
#   FX_BRANCH, FX_DIRTY           git branch and `git status --porcelain` output
#   FX_SSH_EXIT                   exit status of ssh (default 0)
#
# Container names use _ for - in variable names (comics-api -> comics_api).

set -u

name="$(basename "$0")"
STATE="${STUB_STATE:?}"

trace() {
    local line="$name"
    local arg
    for arg in "$@"; do
        line+=" $(printf '%q' "$arg")"
    done
    echo "$line" >> "${TRACE:?}"
}

var() {
    local key="${1//-/_}"
    printf '%s' "${!key-}"
}

fails() {
    [[ " ${FX_FAIL:-} " == *" $1 "* ]]
}

# Count of events such as compose ups, kept across calls in the state dir
counter() {
    local file="$STATE/count-$1"
    cat "$file" 2>/dev/null || echo 0
}

bump() {
    local file="$STATE/count-$1"
    echo $(( $(counter "$1") + 1 )) > "$file"
}

# What the registry reports for image:tag: prints digest|revision|dirty, or fails if it has no such tag
registry_entry() {
    local want="$1"
    local entry
    for entry in ${FX_REGISTRY_INFO:-}; do
        if [[ "${entry%%=*}" == "$want" ]]; then
            echo "${entry#*=}"
            return 0
        fi
    done
    if grep -qx "$want" "$STATE/pushed" 2>/dev/null && ! fails missing-after-push; then
        echo "${FX_PUSHED_INFO:-sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa|abc1234abc1234abc1234abc1234abc1234abc12|false}"
        return 0
    fi
    return 1
}

health() {
    local container="$1"
    local phase
    phase=$(counter up)
    local seq
    seq=$(var "FX_HEALTH_${container}_${phase}")
    if [[ -z "$seq" ]]; then
        [[ -n "$(var "FX_IMAGE_${container}")" ]] || return 1
        echo "no-healthcheck"
        return 0
    fi
    local idx_file="$STATE/health-${container}-${phase}"
    local idx
    idx=$(cat "$idx_file" 2>/dev/null || echo 0)
    local -a statuses
    read -r -a statuses <<< "$seq"
    local last=$(( ${#statuses[@]} - 1 ))
    (( idx > last )) && idx=$last
    echo "${statuses[$idx]}"
    echo $(( idx + 1 )) > "$idx_file"
}

docker_inspect() {
    local format="$2"
    local container="$3"
    local image
    image=$(var "FX_IMAGE_${container}")
    case "$format" in
        *State.Health*)
            health "$container" || { echo "Error: No such object: $container" >&2; return 1; }
            return 0
            ;;
    esac
    [[ -n "$image" ]] || { echo "Error: No such object: $container" >&2; return 1; }
    case "$format" in
        *Config.Image*) echo "$image" ;;
        *com.docker.compose.project*) var "FX_PROJECT_${container}"; echo ;;
        *'.Image }}'*) echo "id-${container}" ;;
        *) echo "stub: unhandled inspect format: $format" >&2; return 1 ;;
    esac
}

docker_stub() {
    case "$1" in
        inspect)
            trace "$@"
            shift
            docker_inspect "$@"
            ;;
        image)
            trace "$@"
            # docker image inspect --format ... id-<container>
            local id="${!#}"
            local digest
            digest=$(var "FX_DIGEST_${id#id-}")
            [[ -n "$digest" ]] && echo "$digest"
            return 0
            ;;
        compose)
            echo "$name API_TAG=${API_TAG-<unset>} UI_TAG=${UI_TAG-<unset>} $(printf '%q ' "$@")" >> "$TRACE"
            local sub
            for sub in "$@"; do
                case "$sub" in
                    config)
                        echo "(rendered compose config)"
                        return 0
                        ;;
                    pull)
                        bump pull
                        fails pull && { echo "stub: pull failed" >&2; return 1; }
                        return 0
                        ;;
                    up)
                        bump up
                        fails "up$(counter up | sed 's/^1$//')" && { echo "stub: up failed" >&2; return 1; }
                        return 0
                        ;;
                esac
            done
            return 0
            ;;
        pull)
            trace "$@"
            fails pull && { echo "stub: pull failed" >&2; return 1; }
            return 0
            ;;
        volume)
            trace "$@"
            # shellcheck disable=SC2086  # FX_VOLUMES is a space separated list
            [[ "$2" == "ls" ]] && [[ -n "${FX_VOLUMES:-}" ]] && printf '%s\n' $FX_VOLUMES
            return 0
            ;;
        run)
            trace "$@"
            bump up
            fails run && { echo "stub: run failed" >&2; return 1; }
            echo "container-id"
            return 0
            ;;
        build)
            echo "$name BUILDAH_FORMAT=${BUILDAH_FORMAT-<unset>} $(printf '%q ' "$@")" >> "$TRACE"
            fails build && return 1
            return 0
            ;;
        save)
            # Records only: the tarball path may be outside the sandbox
            trace "$@"
            return 0
            ;;
        *)
            trace "$@"
            return 0
            ;;
    esac
}

case "$name" in
    docker)
        docker_stub "$@"
        ;;
    ssh)
        trace "$@"
        # Real ssh forwards stdin unless given -n, so record what a call would swallow
        if [[ " $* " != *" -n "* ]] && [[ ! -t 0 ]]; then
            input=$(cat)
            [[ -n "$input" ]] && echo "  (ssh read stdin: $(printf '%q' "$input"))" >> "$TRACE"
        fi
        exit "${FX_SSH_EXIT:-0}"
        ;;
    scp|gradlew|openssl)
        trace "$@"
        [[ "$name" == "openssl" ]] && echo "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        exit 0
        ;;
    skopeo)
        trace "$@"
        if [[ "$1" == "inspect" ]]; then
            # Answers the --format the scripts use: digest|revision|dirty|
            ref="${!#}"
            ref="${ref#docker://registry.stapledon.ca/}"
            if info=$(registry_entry "$ref"); then
                echo "${info}|"
                exit 0
            fi
            echo "stub: manifest unknown: $ref" >&2
            exit 1
        fi
        fails skopeo && exit 1
        # skopeo copy ... docker://<push registry>/<image>:<tag>
        dest="${!#}"
        echo "${dest#docker://*/}" >> "$STATE/pushed"
        exit 0
        ;;
    curl)
        trace "$@"
        url="${!#}"
        if [[ "$url" =~ /v2/(.+)/manifests/(.+)$ ]]; then
            want="${BASH_REMATCH[1]}:${BASH_REMATCH[2]}"
            found=1
            registry_entry "$want" > /dev/null && found=0
            if [[ " $* " == *" -w "* ]]; then
                if [[ $found -eq 0 ]]; then echo -n 200; else echo -n "${FX_REGISTRY_STATUS:-404}"; fi
                exit 0
            fi
            [[ $found -eq 0 ]] && exit 0
            exit 22
        fi
        exit 0
        ;;
    git)
        trace "$@"
        case "$*" in
            *"rev-parse --abbrev-ref HEAD"*) echo "${FX_BRANCH:-master}" ;;
            *"rev-parse --short HEAD"*) echo "abc1234" ;;
            *"rev-parse HEAD"*) echo "abc1234abc1234abc1234abc1234abc1234abc12" ;;
            *"status --porcelain"*) [[ -n "${FX_DIRTY:-}" ]] && echo "$FX_DIRTY" ;;
            *"fetch"*) fails fetch && exit 1 ;;
            *"merge-base --is-ancestor"*)
                all="$*"
                rev="${all##*--is-ancestor }"
                rev="${rev%% *}"
                [[ " ${FX_ON_MASTER:-} " == *" $rev "* ]] && exit 0
                exit 1
                ;;
        esac
        exit 0
        ;;
    hostname)
        echo "dockerhost"
        ;;
    date)
        echo "2026-01-01T00:00:00Z"
        ;;
    sleep)
        :
        ;;
    *)
        echo "stub: no behaviour for $name" >&2
        exit 127
        ;;
esac
