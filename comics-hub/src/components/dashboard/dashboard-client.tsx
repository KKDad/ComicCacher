'use client';

import { useGetComicsQuery, useGetMeQuery, useGetUserPreferencesQuery } from '@/generated/graphql';
import { PageHeader } from '@/components/dashboard/page-header';
import { ContinueReading } from '@/components/dashboard/continue-reading';
import { FavoritesSection } from '@/components/dashboard/favorites-section';
import { TodaysComics } from '@/components/dashboard/todays-comics';

export function DashboardClient() {
  const { data: meData } = useGetMeQuery();

  const { data: comicsData, isLoading: comicsLoading, error: comicsError } = useGetComicsQuery({ first: 20 });

  const { data: prefsData, isLoading: prefsLoading, error: prefsError } = useGetUserPreferencesQuery();

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

  const comics = comicsData?.comics?.edges?.map((e) => e.node) ?? [];
  const prefs = prefsData?.preferences ?? null;

  const todaysComics = comics.map((c) => ({
    id: c.id,
    name: c.name,
    date: c.lastStrip?.date ?? c.newest,
    thumbnail: c.lastStrip?.imageUrl ?? c.avatarUrl ?? undefined,
  }));

  const favoriteIds = new Set(prefs?.favoriteComics ?? []);
  const favorites = comics
    .filter((c) => favoriteIds.has(c.id))
    .map((c) => ({
      id: c.id,
      name: c.name,
      avatarUrl: c.avatarUrl,
    }));

  let lastRead = null;
  if (prefs?.lastReadDates?.length) {
    const sorted = [...prefs.lastReadDates].sort(
      (a, b) => new Date(b.date).getTime() - new Date(a.date).getTime(),
    );
    const most = sorted[0];
    const comic = comics.find((c) => c.id === most.comicId);
    if (comic) {
      lastRead = {
        comic: {
          id: comic.id,
          name: comic.name,
          lastStrip: comic.lastStrip
            ? { imageUrl: comic.lastStrip.imageUrl }
            : null,
        },
        date: most.date,
      };
    }
  }

  const isLoading = comicsLoading || prefsLoading;

  return (
    <div className="space-y-8">
      <PageHeader displayName={meData?.me?.displayName ?? 'there'} />
      <ContinueReading lastRead={lastRead} isLoading={isLoading} />
      <FavoritesSection favorites={favorites.length > 0 ? favorites : null} isLoading={isLoading} />
      <TodaysComics comics={todaysComics.length > 0 ? todaysComics : null} isLoading={comicsLoading} />
    </div>
  );
}
