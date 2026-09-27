import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { type DisplaySettings, DEFAULT_DISPLAY_SETTINGS, mergeWithDefaults } from '@/lib/preferences-defaults';
import { PREFERENCES_STORAGE_KEY } from '@/lib/theme';

// The theme itself is applied to <html> by ThemeSync (and the inline script in the root layout),
// which follow settings.theme; the store only holds the value.

interface PreferencesState {
  settings: DisplaySettings;
  isHydrated: boolean;
  hydrate: (serverSettings: Record<string, unknown> | null | undefined) => void;
  setSettings: (partial: Partial<DisplaySettings>) => void;
}

export const usePreferencesStore = create<PreferencesState>()(
  persist(
    (set, get) => ({
      settings: DEFAULT_DISPLAY_SETTINGS,
      isHydrated: false,
      hydrate: (serverSettings) => {
        set({ settings: mergeWithDefaults(serverSettings), isHydrated: true });
      },
      setSettings: (partial) => {
        set({ settings: { ...get().settings, ...partial } });
      },
    }),
    {
      name: PREFERENCES_STORAGE_KEY,
      // isHydrated means "synced with the server this session", so it isn't persisted
      partialize: (state) => ({ settings: state.settings }),
    },
  ),
);
