# Comics.json Migration Guide

## Problem

The Docker container's `/comics/comics.json` file is missing required fields for comic downloading:
- `source` - Must be either "gocomics" or "comicskingdom"
- `sourceIdentifier` - The comic's identifier on the source website

Without these fields, the application cannot download comics and will skip them during processing.

## Solution

Use the provided `migrate-comics-json.py` script to automatically add the missing fields based on comic names.

## Usage

The migration script is located in the `utils` folder of the ComicCacher project.

### 1. Dry Run (Preview Changes)

First, run the script in dry-run mode to see what changes will be made without modifying any files:

```bash
python3 utils/migrate-comics-json.py /path/to/comics.json --dry-run
```

### 2. Apply Migration

Once you're satisfied with the preview, run the script to apply the changes:

```bash
python3 utils/migrate-comics-json.py /path/to/comics.json
```

This will:
- Create a backup of the original file as `comics.json.backup`
- Add `source` and `sourceIdentifier` fields to each comic
- Overwrite the original file with the migrated version

### 3. Custom Output File

If you want to write to a different file instead of overwriting:

```bash
python3 utils/migrate-comics-json.py /path/to/comics.json -o /path/to/comics-migrated.json
```

## For Docker Container

To migrate the comics.json file inside a Docker container:

### Option 1: Copy, Migrate, Copy Back

```bash
# Copy from container to host
docker cp <container_id>:/comics/comics.json ./comics.json

# Run migration
python3 utils/migrate-comics-json.py ./comics.json

# Copy back to container
docker cp ./comics.json <container_id>:/comics/comics.json

# Restart the container
docker restart <container_id>
```

### Option 2: Use Docker Exec with Volume Mount

If you have the script in a volume or can copy it to the container:

```bash
# Copy script to container
docker cp utils/migrate-comics-json.py <container_id>:/tmp/

# Run migration inside container
docker exec <container_id> python3 /tmp/migrate-comics-json.py /comics/comics.json

# Restart the container
docker restart <container_id>
```

## Supported Comics

The script includes mappings for the following comics:

### GoComics (35 comics)
- Adam At Home, Agnes, AndyCap, BC, CalvinAndHobbes
- Cathy, CitizenDog, Committed, Doonesbury, Drabble
- ForBetterorForWorse, FoxTrot, Frank-And-Ernest, Garfield, GetFuzzy
- Herman, Luann, NonSequitur, Overboard, OvertheHedge
- PCandPixel, Peanuts, PearlsBeforeSwine, Pickles, RealityCheck
- RoseisRose, ScaryGary, Shoe, TheBoondocks, TheBornLoser
- TheDuplex, TheGrizzWells, WizardOfId, WorkingDaze, Ziggy

### Comics Kingdom (7 comics)
- Baby Blues, Beetle Bailey, Dustin, Hagar
- Mother Goose & Grimm, Sherman's Lagoon, Zits

## Troubleshooting

### Comic Not Found

If the script reports "No source mapping found" for a comic:

1. Check the comic name spelling in your comics.json
2. Add a mapping to the script's `GOCOMICS_MAPPING` or `COMICSKINGDOM_MAPPING` dictionaries
3. Verify the comic exists on GoComics or Comics Kingdom

### Manual Addition

To manually add source fields to a comic in the JSON:

```json
{
  "id": 12345,
  "name": "Comic Name",
  "author": "Author Name",
  "oldest": "2019-04-01",
  "newest": "2025-06-10",
  "enabled": true,
  "source": "gocomics",           // Add this
  "sourceIdentifier": "comic-slug" // Add this
}
```

The `sourceIdentifier` is typically the lowercase, hyphenated version of the comic name as it appears in the URL.

## Verification

After migration, check the application logs for successful comic downloads:

```bash
docker logs <container_id> | grep "Processing comic"
```

You should see comics being processed without "No source mapping found" errors.
