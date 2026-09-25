'use client';

import { useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import {
  useGetMeQuery,
  useGetUserPreferencesQuery,
  useAddFavoriteMutation,
  useRemoveFavoriteMutation,
} from '@/generated/graphql';
import { useAllComics } from '@/hooks/use-all-comics';
import { PageHeader } from '@/components/dashboard/page-header';
import { ContinueReading } from '@/components/dashboard/continue-reading';
import { FavoritesSection } from '@/components/dashboard/favorites-section';
import { LatestUpdates } from '@/components/dashboard/latest-updates';
import { usePreferencesStore } from '@/stores/preferences-store';

/** How many comics the "Latest updates" grid shows before "View all". */
const LATEST_LIMIT = 12;

export function DashboardClient() {
  const queryClient = useQueryClient();
  const { data: meData } = useGetMeQuery();

  const { comics, isLoading: comicsLoading, error: comicsError } = useAllComics();

  const { data: prefsData, isLoading: prefsLoading, error: prefsError } = useGetUserPreferencesQuery();

  const hydrate = usePreferencesStore((s) => s.hydrate);
  const { showContinueReading, showFavorites, showRecentlyAdded } = usePreferencesStore((s) => s.settings);

  useEffect(() => {
    if (prefsData?.preferences?.displaySettings !== undefined) {
      hydrate(prefsData.preferences.displaySettings);
    }
  }, [prefsData?.preferences?.displaySettings, hydrate]);

  const addFavorite = useAddFavoriteMutation({
    onSuccess: (data) => {
      if (data.addFavorite.errors.length === 0) {
        queryClient.invalidateQueries({ queryKey: ['GetUserPreferences'] });
      }
    },
  });

  const removeFavorite = useRemoveFavoriteMutation({
    onSuccess: (data) => {
      if (data.removeFavorite.errors.length === 0) {
        queryClient.invalidateQueries({ queryKey: ['GetUserPreferences'] });
      }
    },
  });

  if (comicsError || prefsError) {
    return (
      <div className="flex flex-col items-center justify-center py-20 text-center">
        <h2 className="text-xl font-semibold text-destructive">Failed to load dashboard</h2>
        <p className="mt-2 text-muted-foreground">
          {comicsError instanceof Error ? comicsError.message : prefsError instanceof Error ? prefsError.message : 'An unexpected error occurred.'}
        </p>
      </div>
    );
  }

  const prefs = prefsData?.preferences ?? null;

  const favoriteIds = new Set(prefs?.favoriteComics ?? []);
  const lastReadMap = new Map(
    (prefs?.lastReadDates ?? []).map((entry) => [entry.comicId, entry.date]),
  );

  const latestComics = comics
    .filter((c) => c.lastStrip?.date ?? c.newest)
    .map((c) => {
      const latestDate: string = c.lastStrip?.date ?? c.newest;
      const lastRead = lastReadMap.get(c.id);
      const isFavorite = favoriteIds.has(c.id);

      return {
        id: c.id,
        name: c.name,
        date: latestDate,
        thumbnail: c.lastStrip?.imageUrl ?? c.avatarUrl ?? undefined,
        isNew: !lastRead || latestDate > lastRead,
        isFavorite,
        onToggleFavorite: () => {
          if (isFavorite) {
            removeFavorite.mutate({ comicId: c.id });
          } else {
            addFavorite.mutate({ comicId: c.id });
          }
        },
      };
    })
    // Newest strip first; favorites lead within a day, then alphabetical.
    .sort((a, b) =>
      b.date.localeCompare(a.date)
      || Number(b.isFavorite) - Number(a.isFavorite)
      || a.name.localeCompare(b.name))
    .slice(0, LATEST_LIMIT);

  const favorites = comics
    .filter((c) => favoriteIds.has(c.id))
    .map((c) => ({
      id: c.id,
      name: c.name,
      avatarUrl: c.avatarUrl,
    }))
    .sort((a, b) => a.name.localeCompare(b.name));

  // Last-read dates are strip dates, not reading timestamps, so "continue" means
  // the furthest-along comic that still has unread strips (or, if everything is
  // caught up, the furthest-along one).
  const readEntries = (prefs?.lastReadDates ?? [])
    .map((entry) => ({ entry, comic: comics.find((c) => c.id === entry.comicId) }))
    .filter((r) => r.comic !== undefined)
    .sort((a, b) => b.entry.date.localeCompare(a.entry.date));
  const next = readEntries.find((r) => (r.comic!.newest ?? '') > r.entry.date) ?? readEntries[0];
  const lastRead = next
    ? {
        comic: {
          id: next.comic!.id,
          name: next.comic!.name,
          lastStrip: next.comic!.lastStrip ? { imageUrl: next.comic!.lastStrip.imageUrl } : null,
        },
        date: next.entry.date,
      }
    : null;

  const isLoading = comicsLoading || prefsLoading;

  return (
    <div className="space-y-8">
      <PageHeader displayName={meData?.me?.displayName ?? 'there'} />
      {showContinueReading && (
        <ContinueReading lastRead={lastRead} isLoading={isLoading} />
      )}
      {showFavorites && (
        <FavoritesSection favorites={favorites.length > 0 ? favorites : null} isLoading={isLoading} />
      )}
      {showRecentlyAdded && (
        <LatestUpdates comics={latestComics.length > 0 ? latestComics : null} isLoading={comicsLoading} />
      )}
    </div>
  );
}
