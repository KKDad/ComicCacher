#!/bin/bash

# 1. Configuration
REMOTE_HOST="root@portainer.stapledon.ca"
REMOTE_API_PORT=8888
LOCAL_API_PORT=8888
LOCAL_UI_PORT=8899

# 2. Open an SSH Tunnel in the background
# This maps portainer.stapledon.ca:8888 -> localhost:8888
echo "🔌 Establishing tunnel to Prod API..."
ssh -f -N -L $LOCAL_API_PORT:localhost:$REMOTE_API_PORT $REMOTE_HOST

if [ $? -eq 0 ]; then
  echo "✅ Tunnel Active: Localhost:$LOCAL_API_PORT is now connected to Remote:$REMOTE_API_PORT"
else
  echo "❌ Failed to create tunnel."
  exit 1
fi

# 3. Run the Local UI
# We assume the UI looks for an API URL. 
# You might need to change 'API_BASE_URL' to whatever ENV variable your java/js app expects.
echo "🚀 Starting Local UI..."

# Option A: Run the Docker container you just built locally
# Note: --network host might be needed on Linux, or use host.docker.internal on Mac/Windows
# But since we tunneled to localhost, we usually point the app to the host machine.
docker run --rm -it \
  -p $LOCAL_UI_PORT:8080 \
  -e API_BASE_URL="http://host.docker.internal:$LOCAL_API_PORT" \
  kkdad/comics-ui:latest

# Cleanup: Kill the ssh tunnel when you exit the docker container
pkill -f "ssh -f -N -L $LOCAL_API_PORT"
echo "🔌 Tunnel closed."