#!/bin/sh

# verify-json-files.sh
# Verifies the existence of ComicCacher JSON storage files in a given directory.
# Usage: ./verify-json-files.sh <root_directory>

if [ -z "$1" ]; then
    echo "Usage: $0 <root_directory>"
    exit 1
fi

ROOT_DIR="$1"

if [ ! -d "$ROOT_DIR" ]; then
    echo "Error: Directory '$ROOT_DIR' does not exist."
    exit 1
fi

echo "Verifying JSON files in '$ROOT_DIR'..."

# List of expected files relative to ROOT_DIR
# Space separated string for POSIX compatibility
FILES="comics.json users.json preferences.json batch-executions.json retrieval-status.json last_errors.json access-metrics.json combined-metrics.json stats.json"

MISSING_COUNT=0

for FILE in $FILES; do
    FILE_PATH="$ROOT_DIR/$FILE"
    if [ -f "$FILE_PATH" ]; then
        echo "[OK] Found $FILE"
    else
        # preferences.json might be optional if no user preferences are set yet
        if [ "$FILE" = "preferences.json" ]; then
             echo "[WARNING] Could not find $FILE (might be optional)"
        else
             echo "[MISSING] Could not find $FILE"
             MISSING_COUNT=$((MISSING_COUNT + 1))
        fi
    fi
done

echo "----------------------------------------"
echo "Verifying Sidecar Files..."

# Use a temporary file to track errors from subshells if any occur
FAILURES_LOG=$(mktemp)

# Iterate directories in ROOT_DIR
# Using for loop instead of find to avoid subshell variable scope issues
for COMIC_DIR in "$ROOT_DIR"/*; do
    if [ ! -d "$COMIC_DIR" ]; then continue; fi
    COMIC_NAME=$(basename "$COMIC_DIR")
    
    # Skip hidden dirs or special dirs
    case "$COMIC_NAME" in
        .*|metrics-history|@eaDir)
            continue
            ;;
    esac

    # 1. Check for local stats.json
    # (Optional logic omitted for brevity as usually global stats.json is enough, 
    # but we proceed to check structure)

    # 2. Check for image-hashes.json in year directories
    for YEAR_DIR in "$COMIC_DIR"/*; do
        if [ ! -d "$YEAR_DIR" ]; then continue; fi
        YEAR_NAME=$(basename "$YEAR_DIR")
        
        # Check for 4 digit year name using grep
        if echo "$YEAR_NAME" | grep -qE '^[0-9]{4}$'; then
            
            # Check for images to see if this requires hashes
            HAS_IMAGES=0
            # Check for jpg/png extensions specifically
            if ls "$YEAR_DIR"/*.png "$YEAR_DIR"/*.jpg "$YEAR_DIR"/*.jpeg "$YEAR_DIR"/*.gif >/dev/null 2>&1; then
                HAS_IMAGES=1
            fi

            if [ "$HAS_IMAGES" -eq 1 ]; then
                 if [ ! -f "$YEAR_DIR/image-hashes.json" ]; then
                      echo "[WARNING] image-hashes.json missing in $COMIC_NAME/$YEAR_NAME"
                      # Not counting as strict missing error for now, change if needed
                 fi
                 
                 # 3. Verify Image Metadata Sidecars
                 for IMG_PATH in "$YEAR_DIR"/*.png "$YEAR_DIR"/*.jpg "$YEAR_DIR"/*.jpeg "$YEAR_DIR"/*.gif; do
                      # Check if file exists (loop runs literal string if no match)
                      if [ ! -f "$IMG_PATH" ]; then continue; fi
                      
                      # Strip extension and append .json
                      # Standard POSIX parameter expansion
                      JSON_PATH="${IMG_PATH%.*}.json"
                      
                      if [ ! -f "$JSON_PATH" ]; then
                           echo "[MISSING] Metadata sidecar missing for $COMIC_NAME/$YEAR_NAME/$(basename "$IMG_PATH")"
                           echo "1" >> "$FAILURES_LOG"
                      fi
                 done
            fi
        fi
    done
done

SIDECAR_FAILURES=0
if [ -s "$FAILURES_LOG" ]; then
    SIDECAR_FAILURES=$(wc -l < "$FAILURES_LOG")
fi
rm -f "$FAILURES_LOG"

TOTAL_MISSING=$((MISSING_COUNT + SIDECAR_FAILURES))

echo "----------------------------------------"
if [ "$TOTAL_MISSING" -eq 0 ]; then
    echo "Verification complete. All files ok."
    exit 0
else
    echo "Verification failed. $TOTAL_MISSING strict errors found."
    exit 1
fi
