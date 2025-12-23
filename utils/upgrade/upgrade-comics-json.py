#!/usr/bin/env python3
"""
Migration script to add 'source' and 'sourceIdentifier' fields to comics.json

This script reads a comics.json file and adds the missing source information
based on comic names from the ComicCacher.json bootstrap configuration.
"""

import json
import sys
import os
from typing import Dict, Any, Tuple, Optional

# Mapping of comic names to their source and identifier
# Based on ComicCacher.json configuration
GOCOMICS_MAPPING = {
    "Adam At Home": "adamathome",
    "Agnes": "agnes",
    "AndyCap": "andycapp",
    "BC": "bc",
    "CalvinAndHobbes": "calvinandhobbes",
    "Cathy": "cathy",
    "CitizenDog": "citizendog",
    "Committed": "committed",
    "Doonesbury": "doonesbury",
    "Drabble": "drabble",
    "ForBetterorForWorse": "forbetterorforworse",
    "FoxTrot": "foxtrot",
    "Frank-And-Ernest": "frank-and-ernest",
    "Garfield": "garfield",
    "GetFuzzy": "getfuzzy",
    "Herman": "herman",
    "Luann": "luann",
    "NonSequitur": "nonsequitur",
    "Overboard": "overboard",
    "OvertheHedge": "overthehedge",
    "PCandPixel": "pcandpixel",
    "Peanuts": "peanuts",
    "PearlsBeforeSwine": "pearlsbeforeswine",
    "Pickles": "pickles",
    "RealityCheck": "realitycheck",
    "RoseisRose": "roseisrose",
    "ScaryGary": "scarygary",
    "Shoe": "shoe",
    "TheBoondocks": "boondocks",
    "TheBornLoser": "the-born-loser",
    "TheDuplex": "theduplex",
    "TheGrizzWells": "thegrizzwells",
    "WizardOfId": "wizardofid",
    "WorkingDaze": "working-daze",
    "Ziggy": "ziggy"
}

COMICSKINGDOM_MAPPING = {
    "Baby Blues": "baby-blues",
    "Beetle Bailey": "beetle-bailey-1",
    "Dustin": "dustin",
    "Hagar": "hagar-the-horrible",
    "Mother Goose & Grimm": "mother-goose-grimm",
    "Sherman's Lagoon": "sherman-s-lagoon",
    "Zits": "zits"
}


def normalize_comic_name(name: str) -> str:
    """Normalize comic name for matching (remove spaces, make lowercase)"""
    return name.replace(" ", "").replace("&", "").lower()


def find_source_info(comic_name: str) -> Optional[Tuple[str, str]]:
    """
    Find the source and sourceIdentifier for a comic name.

    Returns:
        Tuple of (source, sourceIdentifier) or None if not found
    """
    # Normalize the input name
    normalized = normalize_comic_name(comic_name)

    # Check GoComics
    for key, value in GOCOMICS_MAPPING.items():
        if normalize_comic_name(key) == normalized:
            return ("gocomics", value)

    # Check Comics Kingdom
    for key, value in COMICSKINGDOM_MAPPING.items():
        if normalize_comic_name(key) == normalized:
            return ("comicskingdom", value)

    return None


def migrate_comics_file(input_path: str, output_path: str = None, dry_run: bool = False):
    """
    Migrate comics.json file by adding source and sourceIdentifier fields.

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
        "updated": 0,
        "already_had_source": 0,
        "not_found": 0,
        "errors": []
    }

    # Process each comic
    for comic_id, comic in data["items"].items():
        stats["total"] += 1
        comic_name = comic.get("name", "Unknown")

        # Check if already has source
        if comic.get("source") and comic.get("sourceIdentifier"):
            print(f"✓ {comic_name}: Already has source info")
            stats["already_had_source"] += 1
            continue

        # Find source info
        source_info = find_source_info(comic_name)

        if source_info:
            source, source_id = source_info
            comic["source"] = source
            comic["sourceIdentifier"] = source_id
            print(f"+ {comic_name}: Added source={source}, sourceIdentifier={source_id}")
            stats["updated"] += 1
        else:
            print(f"⚠ {comic_name}: No source mapping found")
            stats["not_found"] += 1
            stats["errors"].append(comic_name)

    # Print summary
    print("\n" + "="*60)
    print("MIGRATION SUMMARY")
    print("="*60)
    print(f"Total comics: {stats['total']}")
    print(f"Updated: {stats['updated']}")
    print(f"Already had source: {stats['already_had_source']}")
    print(f"Not found: {stats['not_found']}")

    if stats["errors"]:
        print("\nComics without mapping:")
        for name in stats["errors"]:
            print(f"  - {name}")

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

        print("✓ Migration completed successfully!")
        return True
    else:
        print("\nDRY RUN - No files were modified")
        print("Run without --dry-run to apply changes")
        return True


def main():
    """Main entry point"""
    import argparse

    parser = argparse.ArgumentParser(
        description="Migrate comics.json to add source and sourceIdentifier fields"
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
