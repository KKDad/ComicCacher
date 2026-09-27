import type { Theme } from '@/lib/preferences-defaults';

/** localStorage key the preferences store persists under (zustand `persist`). */
export const PREFERENCES_STORAGE_KEY = 'comic-hub-preferences';

export type ResolvedTheme = 'light' | 'dark';

const DARK_QUERY = '(prefers-color-scheme: dark)';

export function resolveTheme(theme: Theme): ResolvedTheme {
  if (theme === 'light' || theme === 'dark') return theme;
  return window.matchMedia(DARK_QUERY).matches ? 'dark' : 'light';
}

export function applyTheme(theme: Theme) {
  const resolved = resolveTheme(theme);
  const root = document.documentElement;
  root.classList.remove('light', 'dark');
  root.classList.add(resolved);
  root.setAttribute('data-theme', resolved);
}

/** Calls `onChange` when the OS switches between light and dark. Returns the unsubscribe. */
export function watchSystemTheme(onChange: () => void): () => void {
  const query = window.matchMedia(DARK_QUERY);
  query.addEventListener('change', onChange);
  return () => query.removeEventListener('change', onChange);
}

/**
 * Runs in <head> on full page loads, before the first paint, so the page never renders in the
 * wrong theme. Mirrors applyTheme, reading the theme from the persisted preferences store.
 * Client-side navigations keep the class; ThemeSync owns it after hydration.
 */
export const THEME_BOOTSTRAP_SCRIPT = `(function(){try{var t;var raw=localStorage.getItem(${JSON.stringify(PREFERENCES_STORAGE_KEY)});if(raw){var s=JSON.parse(raw);t=s&&s.state&&s.state.settings&&s.state.settings.theme}if(t!=='light'&&t!=='dark'){t=window.matchMedia(${JSON.stringify(DARK_QUERY)}).matches?'dark':'light'}var r=document.documentElement;r.classList.remove('light','dark');r.classList.add(t);r.setAttribute('data-theme',t)}catch(e){}})()`;
