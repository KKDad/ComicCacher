#!/usr/bin/env python3
"""
Migration script to fix comic slugs and statuses in comics.json

This script updates production comics.json files with:
1. Corrected sourceIdentifiers for comics with wrong slugs
2. Active status flags for discontinued comics
3. Publication schedules for Sunday-only comics

Changes applied:
- TheDuplex: sourceIdentifier = "duplex" (was auto-generated as "theduplex")
- Mother Goose & Grimm: sourceIdentifier = "mother-goose-and-grimm", source = "gocomics"
- Sherman's Lagoon: sourceIdentifier = "shermanslagoon", source = "gocomics"
- Baby Blues: sourceIdentifier = "babyblues", source = "gocomics"
- FoxTrot: publicationDays = ["SUNDAY"]
- Committed: active = false (ended 2006)
- PC and Pixel: active = false (delisted from GoComics)
"""

import json
import sys
import os
from typing import Dict, Any, List, Optional

# Mapping of comic IDs/names to their fixes
SLUG_FIXES = {
    # Comics with wrong slugs
    "theduplex": "duplex",
    "mothergoose&grimm": "mother-goose-and-grimm",
    "sherman'slagoon": "shermanslagoon",
    "babyblues": "babyblues",  # No change but ensure it's set
}

# Comics that moved from Comics Kingdom to GoComics
MOVED_TO_GOCOMICS = {
    "mother goose & grimm": "mother-goose-and-grimm",
    "sherman's lagoon": "shermanslagoon",
    "baby blues": "babyblues",
}

# Comics that are inactive/discontinued
INACTIVE_COMICS = {
    "committed",
    "pcandpixel",
    "pc and pixel",
}

# Comics with publication schedules
PUBLICATION_SCHEDULES = {
    "foxtrot": ["SUNDAY"],
}


def normalize_name(name: str) -> str:
    """Normalize comic name for matching (remove spaces, lowercase, remove punctuation)"""
    return name.replace(" ", "").replace("&", "").replace("'", "").replace("-", "").lower()


def find_comic_by_name(items: Dict[str, Any], search_name: str) -> Optional[tuple]:
    """
    Find a comic in items by normalized name matching.

    Returns:
        Tuple of (comic_id, comic_data) or None if not found
    """
    normalized_search = normalize_name(search_name)

    for comic_id, comic_data in items.items():
        comic_name = comic_data.get("name", "")
        if normalize_name(comic_name) == normalized_search:
            return (comic_id, comic_data)

    return None


def migrate_comics_file(input_path: str, output_path: str = None, dry_run: bool = False):
    """
    Migrate comics.json file with slug fixes and status updates.

    Args:
        input_path: Path to the input comics.json file
        output_path: Path to write the migrated file (default: overwrite input)
        dry_run: If True, only show what would be changed without writing
    """
    # Read the input file
    print(f"Reading comics from: {input_path}")
    with open(input_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    if "items" not in data:
        print("ERROR: Invalid comics.json format - missing 'items' key")
        return False

    # Track statistics
    stats = {
        "total": 0,
        "slug_fixed": 0,
        "moved_to_gocomics": 0,
        "marked_inactive": 0,
        "schedule_added": 0,
        "no_changes": 0,
        "changes": []
    }

    # Process each comic
    for comic_id, comic in data["items"].items():
        stats["total"] += 1
        comic_name = comic.get("name", "Unknown")
        normalized_name = normalize_name(comic_name)
        source_id = comic.get("sourceIdentifier", "")
        source = comic.get("source", "")
        changed = False
        changes_made = []

        # Fix 1: Update sourceIdentifier for comics with wrong slugs
        if source_id in SLUG_FIXES and SLUG_FIXES[source_id] != source_id:
            old_id = source_id
            new_id = SLUG_FIXES[source_id]
            comic["sourceIdentifier"] = new_id
            changes_made.append(f"sourceIdentifier: {old_id} -> {new_id}")
            stats["slug_fixed"] += 1
            changed = True

        # Fix 2: Comics moved from Comics Kingdom to GoComics
        if comic_name.lower() in MOVED_TO_GOCOMICS:
            new_source_id = MOVED_TO_GOCOMICS[comic_name.lower()]
            old_source = comic.get("source", "")

            if old_source != "gocomics":
                comic["source"] = "gocomics"
                changes_made.append(f"source: {old_source} -> gocomics")
                stats["moved_to_gocomics"] += 1
                changed = True

            if source_id != new_source_id:
                comic["sourceIdentifier"] = new_source_id
                changes_made.append(f"sourceIdentifier: {source_id} -> {new_source_id}")
                if not changed:  # Don't double-count
                    stats["slug_fixed"] += 1
                changed = True

        # Fix 3: Mark inactive comics
        if normalized_name in INACTIVE_COMICS:
            if comic.get("active") != False:
                comic["active"] = False
                changes_made.append("active: true -> false (discontinued)")
                stats["marked_inactive"] += 1
                changed = True

        # Fix 4: Add publication schedules
        if normalized_name in PUBLICATION_SCHEDULES:
            schedule = PUBLICATION_SCHEDULES[normalized_name]
            if "publicationDays" not in comic or comic.get("publicationDays") != schedule:
                comic["publicationDays"] = schedule
                changes_made.append(f"publicationDays: added {schedule}")
                stats["schedule_added"] += 1
                changed = True

        # Report changes
        if changed:
            change_report = f"[OK] {comic_name}: {', '.join(changes_made)}"
            print(change_report)
            stats["changes"].append(change_report)
        else:
            stats["no_changes"] += 1

    # Print summary
    print("\n" + "="*70)
    print("MIGRATION SUMMARY")
    print("="*70)
    print(f"Total comics: {stats['total']}")
    print(f"Slug fixes applied: {stats['slug_fixed']}")
    print(f"Moved to GoComics: {stats['moved_to_gocomics']}")
    print(f"Marked inactive: {stats['marked_inactive']}")
    print(f"Publication schedules added: {stats['schedule_added']}")
    print(f"No changes needed: {stats['no_changes']}")

    if stats["changes"]:
        print(f"\nTotal comics modified: {len(stats['changes'])}")

    # Write output if not dry run
    if not dry_run:
        output_path = output_path or input_path

        # Create backup of original file
        backup_path = input_path + ".backup"
        if os.path.exists(input_path):
            print(f"\nCreating backup: {backup_path}")
            with open(backup_path, 'w', encoding='utf-8') as f:
                with open(input_path, 'r', encoding='utf-8') as orig:
                    f.write(orig.read())

        print(f"Writing migrated comics to: {output_path}")
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)

        print("[SUCCESS] Migration completed successfully!")
        return True
    else:
        print("\nDRY RUN - No files were modified")
        print("Run without --dry-run to apply changes")
        return True


def main():
    """Main entry point"""
    import argparse

    parser = argparse.ArgumentParser(
        description="Migrate comics.json to fix slugs and add status flags"
    )
    parser.add_argument(
        "input_file",
        help="Path to comics.json file to migrate"
    )
    parser.add_argument(
        "-o", "--output",
        help="Output file path (default: overwrite input file)"
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Show what would be changed without modifying files"
    )

    args = parser.parse_args()

    # Check if input file exists
    if not os.path.exists(args.input_file):
        print(f"ERROR: Input file not found: {args.input_file}")
        sys.exit(1)

    # Run migration
    success = migrate_comics_file(args.input_file, args.output, args.dry_run)
    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()
