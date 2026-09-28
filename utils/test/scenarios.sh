# shellcheck shell=bash
# shellcheck disable=SC2034  # the variables are read by run-tests.sh and stub.sh
#
# Scenarios for run-tests.sh: one function each, named sc_<name> (the name in expected.txt uses
# dashes). Each runs in the sandbox shell with every variable exported, just before the script
# under test, and sets:
#   TARGET=repo:utils/build.sh          a script in the repo copy, or
#   TARGET=host:comics-deploy/run.sh    a script staged on the fake Docker host
#   ARGS=(prod --api 2.6.0)             its arguments
#   STDIN=$'y\n'                        what it reads (/dev/null when unset)
#   FX_...                              fixtures for stub.sh (see its header)
# and may create files under $SB (the sandbox root) first.
#

# The prod Docker host: comics-api and comics-ui on 2.5.0, in the comics compose project
prod_host() {

    FX_IMAGE_comics_api=registry.stapledon.ca/kkdad/comic-api:2.5.0
    FX_DIGEST_comics_api=registry.stapledon.ca/kkdad/comic-api@sha256:1111111111111111111111111111111111111111111111111111111111111111
    FX_PROJECT_comics_api=comics
    FX_IMAGE_comics_ui=registry.stapledon.ca/kkdad/comic-ui:2.5.0
    FX_DIGEST_comics_ui=registry.stapledon.ca/kkdad/comic-ui@sha256:2222222222222222222222222222222222222222222222222222222222222222
    FX_PROJECT_comics_ui=comics
}

# build.sh without an environment refuses
sc_build_no_env() {
    TARGET=repo:utils/build.sh
    ARGS=(--api 9.9.9-test)
}

# Build and push API and UI from clean master; both found in the registry
sc_build_prod_both() {
    TARGET=repo:utils/build.sh
    ARGS=(prod --api 9.9.9-test --ui 9.9.9-test)
}

# Refuses to build from a dirty tree
sc_build_prod_dirty() {
    TARGET=repo:utils/build.sh
    ARGS=(prod --api 9.9.9-test)
    FX_DIRTY=" M README.md"
}

# Push reported success but the registry lacks the image
sc_build_prod_not_in_registry() {
    TARGET=repo:utils/build.sh
    ARGS=(prod --api 9.9.9-test)
    FX_FAIL=missing-after-push
}

# Refuses to build from a branch other than master
sc_build_prod_not_master() {
    TARGET=repo:utils/build.sh
    ARGS=(prod --api 9.9.9-test)
    FX_BRANCH=feature/x
}

# Registry error while checking the tag: refuses instead of guessing
sc_build_prod_registry_error() {
    TARGET=repo:utils/build.sh
    ARGS=(prod --api 9.9.9-test)
    FX_REGISTRY_STATUS=500
}

# Replaces a tag that only holds a dev build
sc_build_prod_tag_taken_dev() {
    TARGET=repo:utils/build.sh
    ARGS=(prod --api 9.9.9-test)
    FX_ON_MASTER=abc1234abc1234abc1234abc1234abc1234abc12
    FX_REGISTRY_INFO="kkdad/comic-api:9.9.9-test=sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb|fff0000fff0000fff0000fff0000fff0000fff00|true"
}

# Refuses to replace a tag that holds a build of master
sc_build_prod_tag_taken_release() {
    TARGET=repo:utils/build.sh
    ARGS=(prod --api 9.9.9-test)
    FX_ON_MASTER=abc1234abc1234abc1234abc1234abc1234abc12
    FX_REGISTRY_INFO="kkdad/comic-api:9.9.9-test=sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb|abc1234abc1234abc1234abc1234abc1234abc12|false"
}

# Refuses to replace an unlabelled tag, which may be an old release
sc_build_prod_tag_taken_unlabelled() {
    TARGET=repo:utils/build.sh
    ARGS=(prod --api 9.9.9-test)
    FX_ON_MASTER=abc1234abc1234abc1234abc1234abc1234abc12
    FX_REGISTRY_INFO="kkdad/comic-api:9.9.9-test=sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb||"
}

# A near miss for prod is refused, not treated as dev
sc_deploy_bad_env() {
    TARGET=repo:utils/deploy.sh
    ARGS=(prd --api 9.9.9-test)
}

# The override flag is prod-only
sc_deploy_dev_allow_unverified_refused() {
    TARGET=repo:utils/deploy.sh
    ARGS=(dev --api 9.9.9-test --allow-unverified-image)
}

# Rejects a tag that isn't semver
sc_deploy_dev_bad_tag() {
    TARGET=repo:utils/deploy.sh
    ARGS=(dev --api '2.5.0 && reboot' --skip-build)
}

# Build and deploy the dev API from a feature branch (no gates)
sc_deploy_dev_full() {
    TARGET=repo:utils/deploy.sh
    ARGS=(dev --api 9.9.9-test)
    FX_BRANCH=feature/x
    FX_DIRTY=" M README.md"
}

# Deploy an already-pushed dev image
sc_deploy_dev_skip_build() {
    TARGET=repo:utils/deploy.sh
    ARGS=(dev --api 9.9.9-test --skip-build)
}

# dev has no UI container
sc_deploy_dev_ui_refused() {
    TARGET=repo:utils/deploy.sh
    ARGS=(dev --ui 9.9.9-test)
}

# deploy.sh without an environment refuses before git, build or ssh
sc_deploy_no_env() {
    TARGET=repo:utils/deploy.sh
    ARGS=(--api 9.9.9-test)
}

# Rejects a tag that isn't semver before any ssh
sc_deploy_prod_bad_tag() {
    TARGET=repo:utils/deploy.sh
    ARGS=(prod --api '9.9.9;reboot' --skip-build)
}

# API and UI both verified; one unverified image refuses the whole deploy
sc_deploy_prod_both_pinned() {
    TARGET=repo:utils/deploy.sh
    ARGS=(prod --api 9.9.9-test --ui 9.9.9-test --skip-build)
    FX_ON_MASTER=abc1234abc1234abc1234abc1234abc1234abc12
    FX_REGISTRY_INFO="kkdad/comic-api:9.9.9-test=sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb|abc1234abc1234abc1234abc1234abc1234abc12|false kkdad/comic-ui:9.9.9-test=sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc|fff0000fff0000fff0000fff0000fff0000fff00|false"
}

# API and UI both verified: both pinned by digest
sc_deploy_prod_both_verified() {
    TARGET=repo:utils/deploy.sh
    ARGS=(prod --api 9.9.9-test --ui 9.9.9-test --skip-build)
    FX_ON_MASTER=abc1234abc1234abc1234abc1234abc1234abc12
    FX_REGISTRY_INFO="kkdad/comic-api:9.9.9-test=sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb|abc1234abc1234abc1234abc1234abc1234abc12|false kkdad/comic-ui:9.9.9-test=sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc|abc1234abc1234abc1234abc1234abc1234abc12|false"
}

# Refuses to deploy from a dirty tree
sc_deploy_prod_dirty() {
    TARGET=repo:utils/deploy.sh
    ARGS=(prod --api 9.9.9-test --skip-build)
    FX_DIRTY="?? scratch.txt"
}

# Dry run: no build, stage the files, run.sh --dry-run
sc_deploy_prod_dry_run() {
    TARGET=repo:utils/deploy.sh
    ARGS=(prod --api 9.9.9-test --dry-run)
    FX_ON_MASTER=abc1234abc1234abc1234abc1234abc1234abc12
    FX_REGISTRY_INFO="kkdad/comic-api:9.9.9-test=sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb|abc1234abc1234abc1234abc1234abc1234abc12|false"
}

# Can't fetch origin/master: refuses rather than check against a stale ref
sc_deploy_prod_fetch_fails() {
    TARGET=repo:utils/deploy.sh
    ARGS=(prod --api 9.9.9-test --skip-build)
    FX_FAIL=fetch
    FX_ON_MASTER=abc1234abc1234abc1234abc1234abc1234abc12
    FX_REGISTRY_INFO="kkdad/comic-api:9.9.9-test=sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb|abc1234abc1234abc1234abc1234abc1234abc12|false"
}

# Build, stage and deploy the API; the y piped in shows which ssh reads it
sc_deploy_prod_full() {
    TARGET=repo:utils/deploy.sh
    ARGS=(prod --api 9.9.9-test)
    STDIN=$'y\n'
    FX_ON_MASTER=abc1234abc1234abc1234abc1234abc1234abc12
}

# --skip-build with a tag the registry doesn't have
sc_deploy_prod_missing_image() {
    TARGET=repo:utils/deploy.sh
    ARGS=(prod --api 9.9.9-test --skip-build)
    FX_ON_MASTER=abc1234abc1234abc1234abc1234abc1234abc12
}

# COMPOSE_PROJECT_NAME is passed through to prod-run.sh
sc_deploy_prod_project_override() {
    TARGET=repo:utils/deploy.sh
    ARGS=(prod --api 9.9.9-test --skip-build)
    COMPOSE_PROJECT_NAME=comics2
    FX_ON_MASTER=abc1234abc1234abc1234abc1234abc1234abc12
    FX_REGISTRY_INFO="kkdad/comic-api:9.9.9-test=sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb|abc1234abc1234abc1234abc1234abc1234abc12|false"
}

# --skip-build still requires master
sc_deploy_prod_skip_build_not_master() {
    TARGET=repo:utils/deploy.sh
    ARGS=(prod --ui 9.9.9-test --skip-build)
    FX_BRANCH=feature/x
}

# Skip the build: stage and deploy an already-pushed UI
sc_deploy_prod_skip_build() {
    TARGET=repo:utils/deploy.sh
    ARGS=(prod --ui 9.9.9-test --skip-build)
    FX_ON_MASTER=abc1234abc1234abc1234abc1234abc1234abc12
    FX_REGISTRY_INFO="kkdad/comic-ui:9.9.9-test=sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb|abc1234abc1234abc1234abc1234abc1234abc12|false"
}

# --allow-unverified-image deploys it, pinned, with a warning and provenance=override
sc_deploy_prod_unlabelled_override() {
    TARGET=repo:utils/deploy.sh
    ARGS=(prod --ui 9.9.9-test --skip-build --allow-unverified-image)
    FX_ON_MASTER=abc1234abc1234abc1234abc1234abc1234abc12
    FX_REGISTRY_INFO="kkdad/comic-ui:9.9.9-test=sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb||"
}

# An image without provenance labels (built before them) is refused
sc_deploy_prod_unlabelled() {
    TARGET=repo:utils/deploy.sh
    ARGS=(prod --ui 9.9.9-test --skip-build)
    FX_ON_MASTER=abc1234abc1234abc1234abc1234abc1234abc12
    FX_REGISTRY_INFO="kkdad/comic-ui:9.9.9-test=sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb||"
}

# An image built from a commit that isn't on origin/master is refused before any ssh
sc_deploy_prod_unverified_branch() {
    TARGET=repo:utils/deploy.sh
    ARGS=(prod --api 9.9.9-test --skip-build)
    FX_ON_MASTER=abc1234abc1234abc1234abc1234abc1234abc12
    FX_REGISTRY_INFO="kkdad/comic-api:9.9.9-test=sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb|fff0000fff0000fff0000fff0000fff0000fff00|false"
}

# An image built from a dirty tree is refused, even from a master commit
sc_deploy_prod_unverified_dirty() {
    TARGET=repo:utils/deploy.sh
    ARGS=(prod --api 9.9.9-test --skip-build)
    FX_ON_MASTER=abc1234abc1234abc1234abc1234abc1234abc12
    FX_REGISTRY_INFO="kkdad/comic-api:9.9.9-test=sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb|abc1234abc1234abc1234abc1234abc1234abc12|true"
}

# The line count can't carry a remote command
sc_logs_bad_lines() {
    TARGET=repo:utils/logs.sh
    ARGS=(prod api '10; docker rm -f comics-api')
}

# All of comics-api-dev's log
sc_logs_dev_api_all() {
    TARGET=repo:utils/logs.sh
    ARGS=(dev api all)
}

# dev has no UI container
sc_logs_dev_ui_refused() {
    TARGET=repo:utils/logs.sh
    ARGS=(dev ui)
}

# logs.sh without an environment refuses
sc_logs_no_env() {
    TARGET=repo:utils/logs.sh
    ARGS=()
}

# Default: last 500 lines of comics-api
sc_logs_prod_api() {
    TARGET=repo:utils/logs.sh
    ARGS=(prod)
}

# comics-ui, 100 lines
sc_logs_prod_ui() {
    TARGET=repo:utils/logs.sh
    ARGS=(prod ui 100)
}

# Second compose deploy: container already belongs to comics-dev, so it isn't removed by hand
sc_run_dev_compose_managed() {
    TARGET=host:comics-deploy-dev/run.sh
    ARGS=(dev --api 2.6.1)
    FX_IMAGE_comics_api_dev=registry.stapledon.ca/kkdad/comic-api:2.6.0
    FX_PROJECT_comics_api_dev=comics-dev
    FX_VOLUMES="comicdata-dev"
    FX_HEALTH_comics_api_dev_1="healthy"
    printf 'COMICS_DEVTOKEN_ENABLED=true\nCOMICS_DEVTOKEN_SECRET=s\nCOMICS_DEVTOKEN_DEFAULTUSERNAME=uireview0927\n' \
        > "$SB/host/comics-deploy-dev/dev-token.env"
}

# Dry run prints the docker commands and changes nothing
sc_run_dev_dry_run() {
    TARGET=host:comics-deploy-dev/run.sh
    ARGS=(dev --api 2.6.0 --dry-run)
    FX_IMAGE_comics_api_dev=registry.stapledon.ca/kkdad/comic-api:2.5.0
}

# No dev container, volume or dev-token.env yet
sc_run_dev_first_install() {
    TARGET=host:comics-deploy-dev/run.sh
    ARGS=(dev --api 2.6.0)
    FX_HEALTH_comics_api_dev_1="starting healthy"
}

# A comics-api-dev container from another compose project (here prod's) is left alone
sc_run_dev_foreign_project() {
    TARGET=host:comics-deploy-dev/run.sh
    ARGS=(dev --api 2.6.0)
    FX_IMAGE_comics_api_dev=registry.stapledon.ca/kkdad/comic-api:2.5.0
    FX_PROJECT_comics_api_dev=comics
}

# run.sh dev next to the prod compose file refuses
sc_run_dev_in_prod_dir() {
    prod_host
    TARGET=host:comics-deploy/run.sh
    ARGS=(dev --api 2.6.0)
}

# Pull fails: the running dev container is untouched
sc_run_dev_pull_fails() {
    TARGET=host:comics-deploy-dev/run.sh
    ARGS=(dev --api 2.6.0)
    FX_IMAGE_comics_api_dev=registry.stapledon.ca/kkdad/comic-api:2.5.0
    FX_FAIL=pull
}

# Replace a running dev container; dev-token.env predates the default username
sc_run_dev_replace() {
    TARGET=host:comics-deploy-dev/run.sh
    ARGS=(dev --api 2.6.0)
    FX_IMAGE_comics_api_dev=registry.stapledon.ca/kkdad/comic-api:2.5.0
    FX_VOLUMES="comicdata comicdata-dev"
    FX_HEALTH_comics_api_dev_1="healthy"
    printf 'COMICS_DEVTOKEN_ENABLED=true\nCOMICS_DEVTOKEN_SECRET=existing-secret-kept-as-is\n' \
        > "$SB/host/comics-deploy/dev-token.env"
}

# dev has no UI container
sc_run_dev_ui_refused() {
    TARGET=host:comics-deploy-dev/run.sh
    ARGS=(dev --ui 2.6.0)
}

# New dev container never gets healthy: left running, no rollback
sc_run_dev_unhealthy() {
    TARGET=host:comics-deploy-dev/run.sh
    ARGS=(dev --api 2.6.0)
    FX_IMAGE_comics_api_dev=registry.stapledon.ca/kkdad/comic-api:2.5.0
    FX_VOLUMES="comicdata-dev"
    FX_HEALTH_comics_api_dev_1="unhealthy"
    printf 'COMICS_DEVTOKEN_ENABLED=true\nCOMICS_DEVTOKEN_SECRET=s\nCOMICS_DEVTOKEN_DEFAULTUSERNAME=uireview0927\n' \
        > "$SB/host/comics-deploy/dev-token.env"
}

# Operator answers n at the prompt
sc_run_prod_aborted() {
    prod_host
    TARGET=host:comics-deploy/run.sh
    ARGS=(prod --api 2.6.0)
    STDIN=$'n\n'
}

# Only exactly dev or prod is accepted
sc_run_prod_bad_env() {
    prod_host
    TARGET=host:comics-deploy/run.sh
    ARGS=(PROD --api 2.6.0)
}

# Rejects a ref that isn't semver
sc_run_prod_bad_ref() {
    prod_host
    TARGET=host:comics-deploy/run.sh
    ARGS=(prod --api latest)
}

# Deploy API and UI together
sc_run_prod_both_success() {
    prod_host
    TARGET=host:comics-deploy/run.sh
    ARGS=(prod --api 2.6.0 --ui 2.6.0)
    STDIN=$'y\n'
    FX_HEALTH_comics_api_1="starting healthy"
    FX_HEALTH_comics_ui_1="healthy"
}

# Running image is digest-pinned; deploy another pinned ref
sc_run_prod_digest_pinned() {
    prod_host
    TARGET=host:comics-deploy/run.sh
    ARGS=(prod --api 2.6.0@sha256:9999999999999999999999999999999999999999999999999999999999999999)
    STDIN=$'y\n'
    FX_IMAGE_comics_api=registry.stapledon.ca/kkdad/comic-api:2.5.0@sha256:1111111111111111111111111111111111111111111111111111111111111111
    FX_HEALTH_comics_api_1="healthy"
}

# Dry run prints the plan and changes nothing
sc_run_prod_dry_run() {
    prod_host
    TARGET=host:comics-deploy/run.sh
    ARGS=(prod --api 2.6.0 --ui 2.6.0 --dry-run)
}

# run.sh prod next to the dev compose file refuses
sc_run_prod_in_dev_dir() {
    prod_host
    TARGET=host:comics-deploy-dev/run.sh
    ARGS=(prod --api 2.6.0)
    STDIN=$'y\n'
}

# Refuses to run without --api or --ui
sc_run_prod_no_args() {
    prod_host
    TARGET=host:comics-deploy/run.sh
    ARGS=(prod)
}

# API not running before the deploy and unhealthy after: nothing to roll back to
sc_run_prod_no_baseline() {
    prod_host
    TARGET=host:comics-deploy/run.sh
    ARGS=(prod --api 2.6.0)
    STDIN=$'y\n'
    unset FX_IMAGE_comics_api FX_DIGEST_comics_api FX_PROJECT_comics_api
    FX_HEALTH_comics_api_1="unhealthy"
}

# run.sh without an environment refuses before touching docker
sc_run_prod_no_env() {
    prod_host
    TARGET=host:comics-deploy/run.sh
    ARGS=(--api 2.6.0)
}

# No input at the prompt (e.g. stdin swallowed by an earlier ssh)
sc_run_prod_no_input() {
    prod_host
    TARGET=host:comics-deploy/run.sh
    ARGS=(prod --api 2.6.0)
}

# API and UI carry different compose project labels
sc_run_prod_project_disagree() {
    prod_host
    TARGET=host:comics-deploy/run.sh
    ARGS=(prod --api 2.6.0)
    STDIN=$'y\n'
    FX_PROJECT_comics_ui=other
}

# Running containers belong to another compose project
sc_run_prod_project_mismatch() {
    prod_host
    TARGET=host:comics-deploy/run.sh
    ARGS=(prod --api 2.6.0)
    STDIN=$'y\n'
    FX_PROJECT_comics_api=other
    FX_PROJECT_comics_ui=other
}

# --provenance only takes verified or override
sc_run_prod_provenance_bad() {
    prod_host
    TARGET=host:comics-deploy/run.sh
    ARGS=(prod --api 2.6.0 --provenance trusted)
}

# run.sh records the provenance deploy.sh passes
sc_run_prod_provenance_override() {
    prod_host
    TARGET=host:comics-deploy/run.sh
    ARGS=(prod --api 2.6.0@sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb --provenance override)
    STDIN=$'y\n'
    FX_HEALTH_comics_api_1="healthy"
}

# Pull fails (tag missing from the registry): nothing is recreated
sc_run_prod_pull_fails() {
    prod_host
    TARGET=host:comics-deploy/run.sh
    ARGS=(prod --api 2.6.0)
    STDIN=$'y\n'
    FX_FAIL=pull
}

# New API and the rollback both stay unhealthy
sc_run_prod_rollback_fail() {
    prod_host
    TARGET=host:comics-deploy/run.sh
    ARGS=(prod --api 2.6.0)
    STDIN=$'y\n'
    FX_HEALTH_comics_api_1="unhealthy"
    FX_HEALTH_comics_api_2="unhealthy"
}

# Rollback uses the compose file of the last successful deploy
sc_run_prod_rollback_last_good() {
    prod_host
    TARGET=host:comics-deploy/run.sh
    ARGS=(prod --api 2.6.0)
    STDIN=$'y\n'
    FX_HEALTH_comics_api_1="unhealthy"
    FX_HEALTH_comics_api_2="healthy"
    printf 'name: comics\n# previous version\n' > "$SB/host/comics-deploy/docker-compose.last-good.yml"
}

# New API never gets healthy; rollback to the baseline digest succeeds
sc_run_prod_rollback_ok() {
    prod_host
    TARGET=host:comics-deploy/run.sh
    ARGS=(prod --api 2.6.0)
    STDIN=$'y\n'
    FX_HEALTH_comics_api_1="unhealthy"
    FX_HEALTH_comics_api_2="healthy"
}

# Deploy the API; it reports starting, then healthy
sc_run_prod_success() {
    prod_host
    TARGET=host:comics-deploy/run.sh
    ARGS=(prod --api 2.6.0)
    STDIN=$'y\n'
    FX_HEALTH_comics_api_1="starting healthy"
}

# UI container absent and --ui not given: refuses
sc_run_prod_ui_missing() {
    prod_host
    TARGET=host:comics-deploy/run.sh
    ARGS=(prod --api 2.6.0)
    STDIN=$'y\n'
    unset FX_IMAGE_comics_ui FX_DIGEST_comics_ui FX_PROJECT_comics_ui
}

# compose up fails after the pull
sc_run_prod_up_fails() {
    prod_host
    TARGET=host:comics-deploy/run.sh
    ARGS=(prod --api 2.6.0)
    STDIN=$'y\n'
    FX_FAIL=up
    FX_HEALTH_comics_api_1="unhealthy"
    FX_HEALTH_comics_api_2="healthy"
}

# No tunnel for dev: its API port is exposed
sc_tunnel_dev_refused() {
    TARGET=repo:utils/tunnel.sh
    ARGS=(dev)
}

# Tunnel to the prod API; the stub ssh exits at once
sc_tunnel_prod() {
    TARGET=repo:utils/tunnel.sh
    ARGS=(prod)
}
