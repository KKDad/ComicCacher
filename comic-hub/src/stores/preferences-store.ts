import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { type DisplaySettings, DEFAULT_DISPLAY_SETTINGS, mergeWithDefaults } from '@/lib/preferences-defaults';

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
        const merged = mergeWithDefaults(serverSettings);
        set({ settings: merged, isHydrated: true });
      },
      setSettings: (partial) => {
        set({ settings: { ...get().settings, ...partial } });
      },
    }),
    {
      name: 'comic-hub-preferences',
    },
  ),
);
