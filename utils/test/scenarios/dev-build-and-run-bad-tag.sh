# Rejects a tag that isn't semver
TARGET=repo:utils/dev-build-and-run.sh
ARGS=('2.5.0 && reboot' --skip-build)
