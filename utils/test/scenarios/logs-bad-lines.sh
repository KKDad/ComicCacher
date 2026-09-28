# The line count can't carry a remote command
TARGET=repo:utils/logs.sh
ARGS=(prod api '10; docker rm -f comics-api')
