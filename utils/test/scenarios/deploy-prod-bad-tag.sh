# Rejects a tag that isn't semver before any ssh
TARGET=repo:utils/deploy.sh
ARGS=(prod --api '9.9.9;reboot' --skip-build)
