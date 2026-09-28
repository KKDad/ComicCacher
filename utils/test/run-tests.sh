#!/bin/bash
#
# Characterization tests for the utils deploy scripts.
#
# Each scenario in utils/test/scenarios/ runs one script in a throwaway sandbox and records
# every external command it calls, what it prints, its exit status and the files it writes
# (the prod audit log, dev-token.env). The result must match utils/test/expected/<scenario>.out.
#
# Safety: scripts run under `env -i` with a PATH that holds only stub.sh (for docker, ssh,
# scp, skopeo, curl, git and the rest) and an allowlist of plain local tools. A command with
# no stub is "not found", so nothing can reach the Docker host, the registry or this repo.
#
# Usage:
#   ./utils/test/run-tests.sh                  # run all scenarios
#   ./utils/test/run-tests.sh prod-run-success # run the scenarios whose names match
#   ./utils/test/run-tests.sh --update         # rewrite the expected files (review the diff!)
#
# Scenario files are bash, sourced with every variable exported:
#   TARGET=repo:utils/prod-build.sh   # a script in the repo copy, or
#   TARGET=host:prod-run.sh           # a script staged on the fake Docker host
#   ARGS=(--api 2.6.0)                # arguments
#   STDIN=$'y\n'                      # what the script reads; /dev/null when unset
#   FX_...                            # fixtures for stub.sh (see its header)
#   setup() { ... }                   # optional, runs in the sandbox first ($SB is its root)
#

set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "${HERE}/../.." && pwd)"
SCENARIOS="${HERE}/scenarios"
EXPECTED="${HERE}/expected"

STUBBED=(docker ssh scp skopeo curl git gradlew hostname date sleep openssl)
LOCAL_TOOLS=(bash sh cat sed grep head tail printf mkdir rm touch chmod dirname basename env tr
    sort cut cp mv mktemp ls wc install tee true false)

UPDATE=0
FILTER=()
for arg in "$@"; do
    case "$arg" in
        --update) UPDATE=1 ;;
        -h|--help) sed -n '2,27p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *) FILTER+=("$arg") ;;
    esac
done

# Sandbox: a copy of the scripts under test, a fake Docker host dir, a HOME and the PATH
make_sandbox() {
    local sb="$1"
    mkdir -p "$sb/bin" "$sb/repo/comic-api" "$sb/repo/comic-hub" "$sb/host/comics-deploy" \
        "$sb/home" "$sb/state"
    cp -R "$ROOT/utils" "$sb/repo/utils"
    rm -rf "$sb/repo/utils/test"
    cp "$ROOT/comic-api/build-docker.sh" "$sb/repo/comic-api/"
    cp "$ROOT/comic-hub/build-docker.sh" "$sb/repo/comic-hub/"
    # Staged on the Docker host the way prod-build-and-run.sh / dev-build-and-run.sh do it
    cp "$ROOT/utils/prod-run.sh" "$ROOT/utils/prod/docker-compose.yml" "$ROOT/utils/dev-run.sh" \
        "$sb/host/comics-deploy/"

    local tool real
    for tool in "${STUBBED[@]}"; do
        ln -s "$HERE/stub.sh" "$sb/bin/$tool"
    done
    ln -s "$HERE/stub.sh" "$sb/repo/gradlew"
    for tool in "${LOCAL_TOOLS[@]}"; do
        real=$(command -v "$tool") || { echo "Missing local tool: $tool" >&2; exit 1; }
        ln -s "$real" "$sb/bin/$tool"
    done

    # Belt and braces: every remote-capable command must resolve to the stub
    for tool in "${STUBBED[@]}"; do
        [[ "$(readlink "$sb/bin/$tool")" == "$HERE/stub.sh" ]] || { echo "Unsafe PATH: $tool" >&2; exit 1; }
    done
}

run_scenario() {
    local scenario="$1"
    local sb="$2"
    local out="$3"
    local status=0

    : > "$sb/trace"
    : > "$sb/output"
    # shellcheck disable=SC2016  # the inner script expands its own variables
    env -i PATH="$sb/bin" HOME="$sb/home" USER=tester LANG=C LC_ALL=C \
        TRACE="$sb/trace" STUB_STATE="$sb/state" SB="$sb" \
        /bin/bash -c '
            set -a
            source "$1"
            set +a
            if declare -F setup >/dev/null; then setup; fi
            case "$TARGET" in
                repo:*) target="$SB/repo/${TARGET#repo:}" ;;
                host:*) target="$SB/host/comics-deploy/${TARGET#host:}" ;;
                *) echo "Bad TARGET: $TARGET" >&2; exit 99 ;;
            esac
            [[ -n "${STDIN+x}" ]] && printf "%s" "$STDIN" > "$SB/stdin" || : > "$SB/stdin"
            cd "$SB/repo"
            exec "$target" "${ARGS[@]}" < "$SB/stdin"
        ' _ "$scenario" > "$sb/output" 2>&1 || status=$?

    {
        echo "== exit: $status"
        echo "== trace"
        cat "$sb/trace"
        echo "== output"
        cat "$sb/output"
        local file
        for file in "$sb/home/.comiccacher-prod-deploy.log" "$sb/host/comics-deploy/dev-token.env"; do
            if [[ -f "$file" ]]; then
                echo "== file: ${file#"$sb"/}"
                cat "$file"
            fi
        done
    } | sed -e "s#${sb}#<SB>#g" > "$out"
}

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

pass=0
fail=0
failed=()
for scenario in "$SCENARIOS"/*.sh; do
    name=$(basename "$scenario" .sh)
    if [[ ${#FILTER[@]} -gt 0 ]]; then
        match=0
        for f in "${FILTER[@]}"; do [[ "$name" == *"$f"* ]] && match=1; done
        [[ $match -eq 1 ]] || continue
    fi

    sb="$TMP/$name"
    make_sandbox "$sb"
    actual="$TMP/$name.out"
    run_scenario "$scenario" "$sb" "$actual"

    if [[ $UPDATE -eq 1 ]]; then
        mkdir -p "$EXPECTED"
        cp "$actual" "$EXPECTED/$name.out"
        echo "updated  $name"
    elif diff -u "$EXPECTED/$name.out" "$actual" > "$TMP/$name.diff" 2>&1; then
        pass=$((pass + 1))
        echo "ok       $name"
    else
        fail=$((fail + 1))
        failed+=("$name")
        echo "FAIL     $name"
        cat "$TMP/$name.diff"
    fi
done

if [[ $UPDATE -eq 0 ]]; then
    echo ""
    echo "$pass passed, $fail failed"
    [[ $fail -eq 0 ]] || { printf '  %s\n' "${failed[@]}"; exit 1; }
fi
