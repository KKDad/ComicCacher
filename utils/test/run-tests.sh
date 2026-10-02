#!/bin/bash
#
# Characterization tests for the utils deploy scripts.
#
# Each scenario in scenarios.sh runs one script in a throwaway sandbox and records every
# external command it calls, what it prints, its exit status and the files it writes (the prod
# audit log, dev-token.env, the last-good compose file). The result must match the scenario's
# section of expected.txt. Runs of repeated lines (health polls) are collapsed to one line.
#
# Safety: scripts run under `env -i` with a PATH that holds only stub.sh (for docker, ssh,
# scp, skopeo, curl, git and the rest) and an allowlist of plain local tools. A command with
# no stub is "not found", so nothing can reach the Docker host, the registry or this repo.
#
# Also checks that the dev and prod compose files share no project, container, volume or port.
#
# Usage:
#   ./utils/test/run-tests.sh                  # run all scenarios
#   ./utils/test/run-tests.sh run-prod         # run the scenarios whose names contain run-prod
#   ./utils/test/run-tests.sh --update         # rewrite expected.txt (review the diff!)
#

set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "${HERE}/../.." && pwd)"
SCENARIOS="${HERE}/scenarios.sh"
EXPECTED="${HERE}/expected.txt"

STUBBED=(docker ssh scp skopeo curl git gradlew hostname date sleep openssl)
LOCAL_TOOLS=(bash sh cat sed grep head tail printf mkdir rm touch chmod dirname basename env tr
    sort cut cp mv mktemp ls wc install tee true false)

UPDATE=0
FILTER=()
for arg in "$@"; do
    case "$arg" in
        --update) UPDATE=1 ;;
        -h|--help) sed -n '2,20p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *) FILTER+=("$arg") ;;
    esac
done
if [[ $UPDATE -eq 1 && ${#FILTER[@]} -gt 0 ]]; then
    echo "--update rewrites every scenario; run it without a name filter." >&2
    exit 1
fi

# Sandbox: a copy of the scripts under test, a fake Docker host dir, a HOME and the PATH
make_sandbox() {
    local sb="$1"
    mkdir -p "$sb/bin" "$sb/repo/comic-api" "$sb/repo/comic-hub" "$sb/host/comics-deploy" \
        "$sb/host/comics-deploy-dev" "$sb/home" "$sb/state" "$sb/tmp"
    cp -R "$ROOT/utils" "$sb/repo/utils"
    rm -rf "$sb/repo/utils/test"
    # Staged on the Docker host the way deploy.sh does it
    cp "$ROOT/utils/remote/run.sh" "$ROOT/utils/prod/docker-compose.yml" "$sb/host/comics-deploy/"
    cp "$ROOT/utils/remote/run.sh" "$ROOT/utils/dev/docker-compose.yml" "$sb/host/comics-deploy-dev/"

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

# Collapses runs of lines that differ only in their [t=...s] poll time
collapse_repeats() {
    awk '
        { key = $0; gsub(/\[t= *[0-9]+s\]/, "[t=]", key) }
        key == prev { n++; next }
        { if (n > 0) print "  (+" n " more)"; print; prev = key; n = 0 }
        END { if (n > 0) print "  (+" n " more)" }
    '
}

run_scenario() {
    local fn="$1"
    local sb="$2"
    local out="$3"
    local status=0

    : > "$sb/trace"
    : > "$sb/output"
    # shellcheck disable=SC2016  # the inner script expands its own variables
    env -i PATH="$sb/bin" HOME="$sb/home" USER=tester LANG=C LC_ALL=C TMPDIR="$sb/tmp" \
        TRACE="$sb/trace" STUB_STATE="$sb/state" SB="$sb" \
        /bin/bash -c '
            source "$1"
            set -a
            "$2"
            set +a
            case "$TARGET" in
                repo:*) target="$SB/repo/${TARGET#repo:}" ;;
                host:*) target="$SB/host/${TARGET#host:}" ;;
                *) echo "Bad TARGET: $TARGET" >&2; exit 99 ;;
            esac
            [[ -n "${STDIN+x}" ]] && printf "%s" "$STDIN" > "$SB/stdin" || : > "$SB/stdin"
            cd "$SB/repo"
            exec "$target" "${ARGS[@]}" < "$SB/stdin"
        ' _ "$SCENARIOS" "$fn" > "$sb/output" 2>&1 || status=$?

    {
        echo "== exit: $status"
        echo "== trace"
        cat "$sb/trace"
        echo "== output"
        cat "$sb/output"
        local file
        for file in "$sb/home/.comiccacher-prod-deploy.log" "$sb"/host/*/dev-token.env "$sb/host/comics-promotion.env"; do
            if [[ -f "$file" ]]; then
                echo "== file: ${file#"$sb"/}"
                cat "$file"
            fi
        done
        for file in "$sb"/host/*/docker-compose.last-good.yml; do
            if [[ -f "$file" ]]; then
                echo "== file: ${file#"$sb"/} (copy of $(basename "$(dirname "$file")")/docker-compose.yml)"
                cmp -s "$file" "$(dirname "$file")/docker-compose.yml" || echo "   differs from docker-compose.yml!"
            fi
        done
        if [[ -n "$(ls -A "$sb/tmp")" ]]; then
            echo "== left in TMPDIR"
            ls -A "$sb/tmp"
        fi
    } | sed -e "s#${sb}/tmp/comic-build\\.[A-Za-z0-9]*#<WORK>#g" -e "s#${sb}#<SB>#g" \
      | collapse_repeats > "$out"
}

# Prints one scenario's section of expected.txt
expected_section() {
    awk -v want="### $1" '
        $0 == want { on = 1; next }
        /^### / { on = 0 }
        on
    ' "$EXPECTED"
}

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

pass=0
fail=0
failed=()

# Dev and prod share one Docker host: their compose files must not share a project, container,
# volume or host port, or a dev deploy could replace a prod container
compose_names() {
    local file="$1"
    sed -n 's/^name:[[:space:]]*/project /p' "$file"
    sed -n 's/^[[:space:]]*container_name:[[:space:]]*/container /p' "$file"
    # Volume keys and their name: overrides, under the top-level volumes: block
    sed -n '/^volumes:/,/^[a-z]/{
        s/^  \([A-Za-z0-9_.-]*\):[[:space:]]*$/volume \1/p
        s/^    name:[[:space:]]*/volume /p
    }' "$file"
    sed -n 's/^[[:space:]]*-[[:space:]]*"\{0,1\}\([0-9]*\):[0-9]*"\{0,1\}[[:space:]]*$/port \1/p' "$file"
}

check_compose_isolation() {
    local dev="$ROOT/utils/dev/docker-compose.yml"
    local prod="$ROOT/utils/prod/docker-compose.yml"
    local shared
    compose_names "$dev" | sort -u > "$TMP/dev-names"
    compose_names "$prod" | sort -u > "$TMP/prod-names"
    shared=$(comm -12 "$TMP/dev-names" "$TMP/prod-names")
    if [[ -n "$shared" ]] || ! grep -qx 'project comics-dev' "$TMP/dev-names" \
            || ! grep -qx 'project comics' "$TMP/prod-names" || ! grep -q '^port ' "$TMP/dev-names"; then
        fail=$((fail + 1))
        failed+=("compose-isolation")
        echo "FAIL     compose-isolation"
        echo "  dev:  $(tr '\n' ',' < "$TMP/dev-names")"
        echo "  prod: $(tr '\n' ',' < "$TMP/prod-names")"
        [[ -z "$shared" ]] || echo "  shared: $shared"
    else
        pass=$((pass + 1))
        echo "ok       compose-isolation"
    fi
}

if [[ $UPDATE -eq 0 && ${#FILTER[@]} -eq 0 ]]; then
    check_compose_isolation
fi

# shellcheck source=utils/test/scenarios.sh
SCENARIO_FNS=$(bash -c 'source "$1"; declare -F | sed -n "s/^declare -f sc_//p"' _ "$SCENARIOS" | sort)
[[ -n "$SCENARIO_FNS" ]] || { echo "No scenarios found in $SCENARIOS" >&2; exit 1; }

: > "$TMP/all.out"
for fn in $SCENARIO_FNS; do
    name="${fn//_/-}"
    if [[ ${#FILTER[@]} -gt 0 ]]; then
        match=0
        for f in "${FILTER[@]}"; do [[ "$name" == *"$f"* ]] && match=1; done
        [[ $match -eq 1 ]] || continue
    fi

    sb="$TMP/$name"
    make_sandbox "$sb"
    actual="$TMP/$name.out"
    run_scenario "sc_$fn" "$sb" "$actual"
    rm -rf "$sb"

    if [[ $UPDATE -eq 1 ]]; then
        { echo "### $name"; cat "$actual"; } >> "$TMP/all.out"
    elif diff -u --label "expected $name" --label "actual $name" \
            <(expected_section "$name") "$actual" > "$TMP/$name.diff" 2>&1; then
        pass=$((pass + 1))
        echo "ok       $name"
    else
        fail=$((fail + 1))
        failed+=("$name")
        echo "FAIL     $name"
        cat "$TMP/$name.diff"
    fi
done

if [[ $UPDATE -eq 1 ]]; then
    {
        echo "# Baselines for utils/test/run-tests.sh: one ### section per scenario in scenarios.sh."
        echo "# Written by run-tests.sh --update; review every diff to this file."
        cat "$TMP/all.out"
    } > "$EXPECTED"
    echo "updated  $EXPECTED"
else
    echo ""
    echo "$pass passed, $fail failed"
    [[ $fail -eq 0 ]] || { printf '  %s\n' "${failed[@]}"; exit 1; }
fi
