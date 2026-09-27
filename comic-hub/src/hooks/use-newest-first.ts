'use client';

import { usePreferencesStore } from '@/stores/preferences-store';
import { useHydrated } from './use-hydrated';

/**
 * True when the reader shows the newest strip at the top and scrolls back through older
 * ones (the "Newest first" scroll order). False for "Catch up", which reads oldest to
 * newest. Held back until hydration so the server HTML and first client render agree.
 */
export function useNewestFirst(): boolean {
  const hydrated = useHydrated();
  const order = usePreferencesStore((s) => s.settings.readerScrollOrder);
  return hydrated && order === 'newest-first';
}
