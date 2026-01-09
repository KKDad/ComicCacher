#!/bin/bash

# Configuration
REMOTE_HOST="root@portainer.stapledon.ca" # Or 10.0.0.47

# Parse arguments
SERVICE=${1:-api}
LINES=${2:-500}

# Determine container name based on service
case "$SERVICE" in
  api)
    CONTAINER_NAME="comics-api"
    DESCRIPTION="Java Spring Boot API"
    ;;
  ui)
    CONTAINER_NAME="comics-ui"
    DESCRIPTION="Angular Web UI (nginx)"
    ;;
  *)
    echo "Usage: $0 [api|ui] [lines]"
    echo "  api  - Fetch logs from comics-api container (default)"
    echo "  ui   - Fetch logs from comics-ui container"
    echo "  lines - Number of log lines to fetch (default: 500)"
    exit 1
    ;;
esac

echo "Fetching last $LINES lines from $CONTAINER_NAME on $REMOTE_HOST..."

# Fetch logs via SSH
# We use '2>&1' to capture both stdout and stderr (application logs often go to stderr)
LOG_CONTENT=$(ssh "$REMOTE_HOST" "docker logs --tail $LINES $CONTAINER_NAME 2>&1")

# Create a prompt wrapper for the LLM
echo "--------------------------------------------------------"
echo "I am debugging a $DESCRIPTION."
echo "Below are the last $LINES lines of logs from the production container ($CONTAINER_NAME)."
echo "--------------------------------------------------------"
echo ""
echo "$LOG_CONTENT"

# Optional: If on macOS, uncomment the line below to copy directly to clipboard
# echo "$LOG_CONTENT" | pbcopy
# echo ">> Logs copied to clipboard!"
