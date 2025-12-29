#!/bin/bash

# Configuration
REMOTE_HOST="root@portainer.stapledon.ca"
REMOTE_API_PORT=8888
LOCAL_API_PORT=8888

# Cleanup function to kill the tunnel
cleanup() {
  echo ""
  echo "🔌 Closing tunnel..."
  pkill -f "ssh -f -N -L $LOCAL_API_PORT:localhost:$REMOTE_API_PORT $REMOTE_HOST"
  exit 0
}

# Set up trap to cleanup on SIGINT (Control-C) or SIGTERM
trap cleanup SIGINT SIGTERM

# Open an SSH Tunnel in the background
# This maps portainer.stapledon.ca:8888 -> localhost:8888
echo "🔌 Establishing tunnel to Prod API..."
ssh -f -N -L $LOCAL_API_PORT:localhost:$REMOTE_API_PORT $REMOTE_HOST

if [ $? -eq 0 ]; then
  echo "✅ Tunnel Active: localhost:$LOCAL_API_PORT -> $REMOTE_HOST:$REMOTE_API_PORT"
  echo "📝 You can now run the UI in another terminal"
  echo "⚠️  Press Control-C to close the tunnel"
  echo ""

  # Keep the script running until interrupted
  while true; do
    sleep 1
  done
else
  echo "❌ Failed to create tunnel."
  exit 1
fi