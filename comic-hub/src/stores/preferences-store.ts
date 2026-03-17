import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { type DisplaySettings, DEFAULT_DISPLAY_SETTINGS, mergeWithDefaults } from '@/lib/preferences-defaults';

function applyTheme(theme: DisplaySettings['theme']) {
  const root = document.documentElement;
  root.classList.remove('light', 'dark');

  if (theme === 'system') {
    const systemTheme = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    root.classList.add(systemTheme);
    root.setAttribute('data-theme', systemTheme);
  } else {
    root.classList.add(theme);
    root.setAttribute('data-theme', theme);
  }
}

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
        applyTheme(merged.theme);
      },
      setSettings: (partial) => {
        const next = { ...get().settings, ...partial };
        set({ settings: next });
        if (partial.theme !== undefined) {
          applyTheme(next.theme);
        }
      },
    }),
    {
      name: 'comic-hub-preferences',
    },
  ),
);
