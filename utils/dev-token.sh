#!/bin/bash
#
# Mint an access + refresh token for a user on the DEV instance (comics-api-dev), for UI and API testing.
#
# Runs on a workstation:
#   - Reads COMICS_DEVTOKEN_SECRET from dev-token.env on the Docker host over ssh (never printed)
#   - Calls the dev-only devToken mutation (see "Dev Tokens" in docs/api/overview.md)
#   - Prints {"token": ..., "refreshToken": ...} as JSON on stdout
#
# Dev only, by design: the endpoint is fixed to comics-api-dev and can't be overridden. Production has no
# devToken mutation, and tokens for production accounts must not be minted or copied.
#
# To sign in the local UI (utils/dev-ui.sh), set the comic-hub-jwt and comic-hub-refresh cookies on
# localhost:3000 to the two values.
#
# Usage:
#   ./utils/dev-token.sh                  # the default test account (uireview0927, USER role)
#   ./utils/dev-token.sh --user adrian    # another dev account, e.g. an admin for the operations pages
#

set -euo pipefail

DEV_HOST="${DEV_HOST:-root@portainer.stapledon.ca}"
DEV_API_URL="http://portainer.stapledon.ca:8087/graphql"
SECRET_FILE="/root/comics-deploy/dev-token.env"
SSH_OPTS="-o ConnectTimeout=10"

usage() {
    cat <<EOF
Usage: $0 [--user <username>]

Prints a token pair for a dev account as JSON.

  --user <name>  Dev account to mint for (default: the API's default, uireview0927)
  -h, --help     Show this help

Environment overrides:
  DEV_HOST   ssh target of the Docker host (default: ${DEV_HOST})
EOF
}

die() {
    echo "Error: $*" >&2
    exit 1
}

USERNAME=""
while [[ $# -gt 0 ]]; do
    case "$1" in
        --user)
            [[ $# -ge 2 ]] || die "--user needs a username"
            USERNAME="$2"
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            usage >&2
            exit 1
            ;;
    esac
done

command -v jq >/dev/null || die "jq is required"

# shellcheck disable=SC2086
SECRET="$(ssh -n $SSH_OPTS "$DEV_HOST" "sed -n 's/^COMICS_DEVTOKEN_SECRET=//p' '${SECRET_FILE}'")" \
    || die "could not read the dev token secret from ${DEV_HOST}"
[[ -n "$SECRET" ]] || die "COMICS_DEVTOKEN_SECRET is not set in ${DEV_HOST}:${SECRET_FILE}"

# The request body goes through stdin so the secret never appears in a process list
RESPONSE="$(jq -n --arg secret "$SECRET" --arg username "$USERNAME" '{
    query: "mutation($secret: String!, $username: String) { devToken(secret: $secret, username: $username) { token refreshToken } }",
    variables: { secret: $secret, username: (if $username == "" then null else $username end) }
}' | curl -sS --fail-with-body -m 15 -H 'Content-Type: application/json' --data @- "$DEV_API_URL")" \
    || die "request to ${DEV_API_URL} failed: ${RESPONSE:-no response}"

if ! jq -e '.data.devToken.token' >/dev/null <<<"$RESPONSE"; then
    die "devToken failed: $(jq -c '.errors // .' <<<"$RESPONSE")"
fi

jq '.data.devToken' <<<"$RESPONSE"
