import { ImageDto } from './image';

/**
 * Result of a comic strip navigation request.
 * Contains the image if found, or helpful metadata about why it wasn't found
 * and what dates are available.
 */
export interface ComicNavigationResult {
  /**
   * Whether the requested image was found
   */
  found: boolean;

  /**
   * The image data (null if not found)
   */
  image: ImageDto | null;

  /**
   * Human-readable reason when image not found
   * Possible values: "AT_END", "AT_BEGINNING", "NO_COMICS_AVAILABLE", "ERROR"
   */
  reason?: string;

  /**
   * The date that was requested
   */
  requestedDate?: string;

  /**
   * Nearest available date going backward from the requested date (null if none)
   */
  nearestPreviousDate?: string | null;

  /**
   * Nearest available date going forward from the requested date (null if none)
   */
  nearestNextDate?: string | null;

  /**
   * The date of the current image being displayed (same as image.imageDate when found)
   */
  currentDate?: string;
}
