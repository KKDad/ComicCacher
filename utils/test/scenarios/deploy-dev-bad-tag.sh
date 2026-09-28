# Rejects a tag that isn't semver
TARGET=repo:utils/deploy.sh
ARGS=(dev --api '2.5.0 && reboot' --skip-build)
