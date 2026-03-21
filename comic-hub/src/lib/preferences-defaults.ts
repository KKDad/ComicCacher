export type Theme = 'light' | 'dark' | 'system';
export type ReaderNavMode = 'favorites' | 'all';
export type ReaderScrollOrder = 'catchup' | 'newest-first';

export interface DisplaySettings {
  theme: Theme;
  showContinueReading: boolean;
  showFavorites: boolean;
  showRecentlyAdded: boolean;
  comicsPerPage: number;
  defaultZoom: number;
  readerNavMode: ReaderNavMode;
  readerScrollOrder: ReaderScrollOrder;
}

export const DEFAULT_DISPLAY_SETTINGS: DisplaySettings = {
  theme: 'system',
  showContinueReading: true,
  showFavorites: true,
  showRecentlyAdded: true,
  comicsPerPage: 24,
  defaultZoom: 100,
  readerNavMode: 'favorites',
  readerScrollOrder: 'catchup',
};

export function mergeWithDefaults(partial: Record<string, unknown> | null | undefined): DisplaySettings {
  return { ...DEFAULT_DISPLAY_SETTINGS, ...partial };
}
