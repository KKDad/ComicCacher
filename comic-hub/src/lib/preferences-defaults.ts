export type Theme = 'light' | 'dark' | 'system';
export type ReadingDirection = 'newest-first' | 'oldest-first';

export interface DisplaySettings {
  theme: Theme;
  showContinueReading: boolean;
  showFavorites: boolean;
  showRecentlyAdded: boolean;
  readingDirection: ReadingDirection;
  comicsPerPage: number;
  defaultZoom: number;
}

export const DEFAULT_DISPLAY_SETTINGS: DisplaySettings = {
  theme: 'system',
  showContinueReading: true,
  showFavorites: true,
  showRecentlyAdded: true,
  readingDirection: 'newest-first',
  comicsPerPage: 24,
  defaultZoom: 100,
};

export function mergeWithDefaults(partial: Record<string, unknown> | null | undefined): DisplaySettings {
  return { ...DEFAULT_DISPLAY_SETTINGS, ...partial };
}
