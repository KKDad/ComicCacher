#!/bin/bash

# Script to generate OpenAPI documentation for ComicAPI
# This script starts the Spring Boot application with the 'apidocs' profile,
# waits for it to start, fetches the OpenAPI JSON, and then stops the application.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_DIR="${SCRIPT_DIR}/../docs"
OUTPUT_FILE="${OUTPUT_DIR}/openapi.json"

# Make sure the output directory exists
mkdir -p "${OUTPUT_DIR}"

echo "Starting Spring Boot application with apidocs profile..."
# Start the application with the apidocs profile
${SCRIPT_DIR}/../gradlew :comic-api:bootRun --args='--spring.profiles.active=apidocs' > /tmp/spring-boot.log 2>&1 &
PID=$!

# Wait for the application to start (up to 30 seconds)
echo "Waiting for application to start..."
MAX_WAIT=30
COUNT=0
while ! curl -s http://localhost:8888/actuator/health &>/dev/null; do
    sleep 1
    ((COUNT++))
    if [ $COUNT -ge $MAX_WAIT ]; then
        echo "Timeout waiting for application to start."
        kill $PID
        exit 1
    fi
    if ! ps -p $PID > /dev/null; then
        echo "Application failed to start. Check /tmp/spring-boot.log for details."
        exit 1
    fi
    echo -n "."
done
echo ""

# Give it a little more time to fully initialize
sleep 2

# Fetch the OpenAPI JSON
echo "Fetching OpenAPI documentation..."
if curl -s http://localhost:8888/v3/api-docs -o "${OUTPUT_FILE}"; then
    echo "OpenAPI documentation saved to ${OUTPUT_FILE}"
else
    echo "Failed to fetch OpenAPI documentation."
    kill $PID
    exit 1
fi

# Stop the application
echo "Shutting down application..."
kill $PID

echo "Done!"