'use client';

import { useLayoutEffect } from 'react';
import { usePreferencesStore } from '@/stores/preferences-store';
import { applyTheme, watchSystemTheme } from '@/lib/theme';

/**
 * Keeps the theme class on <html> in step with the preferences store. The inline script in the
 * root layout handles the first paint; this re-applies it after hydration (dev Strict Mode resets
 * <html> attributes on remount), on every theme change, and when the OS theme changes while the
 * preference is "system".
 */
export function ThemeSync() {
  const theme = usePreferencesStore((s) => s.settings.theme);

  useLayoutEffect(() => {
    applyTheme(theme);
    if (theme !== 'system') return;
    return watchSystemTheme(() => applyTheme('system'));
  }, [theme]);

  return null;
}
