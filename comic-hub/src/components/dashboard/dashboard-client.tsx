'use client';

import { useGetMeQuery, useGetUserPreferencesQuery } from '@/generated/graphql';
import { useAllComics } from '@/hooks/use-all-comics';
import { useFavorites } from '@/hooks/use-favorite';
import { compareByName } from '@/lib/sort';
import { PageHeader } from '@/components/dashboard/page-header';
import { ContinueReading } from '@/components/dashboard/continue-reading';
import { FavoritesSection } from '@/components/dashboard/favorites-section';
import { LatestUpdates } from '@/components/dashboard/latest-updates';
import { usePreferencesStore } from '@/stores/preferences-store';

/** How many comics the "Latest updates" grid shows before "View all". */
const LATEST_LIMIT = 12;
/** How many comics "Continue where you left off" shows. */
const RECENT_LIMIT = 3;

export function DashboardClient() {
  const { data: meData } = useGetMeQuery();

  const { comics, isLoading: comicsLoading, error: comicsError } = useAllComics();

  const { data: prefsData, isLoading: prefsLoading, error: prefsError } = useGetUserPreferencesQuery();

  const { showContinueReading, showFavorites, showRecentlyAdded } = usePreferencesStore((s) => s.settings);

  const { favoriteIds, toggle: toggleFavorite } = useFavorites();

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
        // New since you last read it; a comic you haven't started isn't flagged
        isNew: lastRead !== undefined && latestDate > lastRead,
        isFavorite,
        onToggleFavorite: () => toggleFavorite(c.id),
      };
    })
    // Newest strip first, then alphabetical. Favoriting doesn't reorder the grid,
    // so a heart click never moves the next card out from under the pointer.
    .sort((a, b) => b.date.localeCompare(a.date) || compareByName(a, b))
    .slice(0, LATEST_LIMIT);

  const favorites = comics
    .filter((c) => favoriteIds.has(c.id))
    .map((c) => ({
      id: c.id,
      name: c.name,
      avatarUrl: c.avatarUrl,
    }))
    .sort(compareByName);

  // Last-read dates are strip dates, not reading timestamps, so "continue" lists the
  // furthest-along comics that still have unread strips first, then caught-up ones.
  const recentReads = (prefs?.lastReadDates ?? [])
    .flatMap((entry) => {
      const comic = comics.find((c) => c.id === entry.comicId);
      return comic ? [{ entry, comic, unread: (comic.newest ?? '') > entry.date }] : [];
    })
    .sort((a, b) => Number(b.unread) - Number(a.unread) || b.entry.date.localeCompare(a.entry.date))
    .slice(0, RECENT_LIMIT)
    .map(({ entry, comic, unread }) => ({
      comic: {
        id: comic.id,
        name: comic.name,
        lastStrip: comic.lastStrip ? { imageUrl: comic.lastStrip.imageUrl } : null,
      },
      date: entry.date,
      caughtUp: !unread,
    }));

  const isLoading = comicsLoading || prefsLoading;

  return (
    <div className="space-y-8">
      <PageHeader displayName={meData?.me?.displayName ?? 'there'} />
      {showContinueReading && (
        <ContinueReading reads={recentReads} isLoading={isLoading} />
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
