#!/bin/bash

# Configuration
REMOTE_HOST="root@portainer.stapledon.ca" # Or 10.0.0.47
CONTAINER_NAME="comics-api"
LINES=${1:-500}

echo "Fetching last $LINES lines from $CONTAINER_NAME on $REMOTE_HOST..."

# Fetch logs via SSH
# We use '2>&1' to capture both stdout and stderr (application logs often go to stderr)
LOG_CONTENT=$(ssh "$REMOTE_HOST" "docker logs --tail $LINES $CONTAINER_NAME 2>&1")

# Create a prompt wrapper for the LLM
echo "--------------------------------------------------------"
echo "I am debugging a Java Spring Boot API."
echo "Below are the last $LINES lines of logs from the production container."
echo "--------------------------------------------------------"
echo ""
echo "$LOG_CONTENT"

# Optional: If on macOS, uncomment the line below to copy directly to clipboard
# echo "$LOG_CONTENT" | pbcopy
# echo ">> Logs copied to clipboard!"
