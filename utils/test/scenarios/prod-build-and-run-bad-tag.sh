# Rejects a tag that isn't semver before any ssh
TARGET=repo:utils/prod-build-and-run.sh
ARGS=(--api '9.9.9;reboot' --skip-build)
