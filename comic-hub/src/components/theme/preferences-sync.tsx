'use client';

import { useEffect } from 'react';
import { useGetUserPreferencesQuery } from '@/generated/graphql';
import { usePreferencesStore } from '@/stores/preferences-store';

/**
 * Loads the signed-in user's saved display settings into the preferences store on every
 * authenticated route, so the theme and reader settings follow the account rather than
 * only the pages that happen to fetch preferences.
 */
export function PreferencesSync() {
  const { data } = useGetUserPreferencesQuery();
  const hydrate = usePreferencesStore((s) => s.hydrate);
  const displaySettings = data?.preferences?.displaySettings;

  useEffect(() => {
    if (displaySettings !== undefined) {
      hydrate(displaySettings);
    }
  }, [displaySettings, hydrate]);

  return null;
}
