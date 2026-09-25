'use client';

import { useEffect } from 'react';
import { useTheme } from 'next-themes';
import { usePreferencesStore } from '@/stores/preferences-store';

/**
 * Pushes the saved theme preference into next-themes. next-themes owns the
 * <html> class and its own localStorage copy, which its inline script applies
 * before first paint; this keeps that copy in step with the server preference
 * (set on another device, or changed on the Preferences page).
 */
export function ThemeSync() {
  const { setTheme } = useTheme();
  const theme = usePreferencesStore((s) => s.settings.theme);
  const isHydrated = usePreferencesStore((s) => s.isHydrated);

  useEffect(() => {
    if (isHydrated) setTheme(theme);
  }, [isHydrated, theme, setTheme]);

  return null;
}
