'use client';

import { useQuery } from '@tanstack/react-query';
import { fetcher } from '@/lib/graphql-client';
import { PageHeader } from '@/components/dashboard/page-header';
import { ContinueReading } from '@/components/dashboard/continue-reading';
import { FavoritesSection } from '@/components/dashboard/favorites-section';
import { TodaysComics } from '@/components/dashboard/todays-comics';

const GET_COMICS = `
  query GetComics($first: Int) {
    comics(first: $first) {
      edges {
        node {
          id
          name
          newest
          avatarUrl
          lastStrip {
            imageUrl
            date
          }
        }
      }
    }
  }
`;

const GET_ME = `
  query GetMe {
    me {
      displayName
    }
  }
`;

const GET_USER_PREFERENCES = `
  query GetUserPreferences {
    preferences {
      favoriteComics
      lastReadDates {
        comicId
        date
      }
    }
  }
`;

interface ComicNode {
  id: number;
  name: string;
  newest: string;
  avatarUrl?: string | null;
  lastStrip?: { imageUrl?: string | null; date?: string | null } | null;
}

interface ComicsResult {
  comics: {
    edges: { node: ComicNode }[];
  };
}

interface PreferencesResult {
  preferences: {
    favoriteComics: number[];
    lastReadDates: { comicId: number; date: string }[];
  };
}

interface MeResult {
  me: { displayName: string };
}

export function DashboardClient() {
  const { data: meData } = useQuery({
    queryKey: ['me'],
    queryFn: fetcher<MeResult, never>(GET_ME),
  });

  const { data: comicsData, isLoading: comicsLoading, error: comicsError } = useQuery({
    queryKey: ['comics'],
    queryFn: fetcher<ComicsResult, { first: number }>(GET_COMICS, { first: 20 }),
  });

  const { data: prefsData, isLoading: prefsLoading, error: prefsError } = useQuery({
    queryKey: ['preferences'],
    queryFn: fetcher<PreferencesResult, never>(GET_USER_PREFERENCES),
  });

  if (comicsError || prefsError) {
    return (
      <div className="flex flex-col items-center justify-center py-20 text-center">
        <h2 className="text-xl font-semibold text-destructive">Failed to load dashboard</h2>
        <p className="mt-2 text-muted-foreground">
          {comicsError?.message || prefsError?.message || 'An unexpected error occurred.'}
        </p>
      </div>
    );
  }

  const comics = comicsData?.comics?.edges?.map((e) => e.node) ?? [];
  const prefs = prefsData?.preferences ?? null;

  const todaysComics = comics.map((c) => ({
    id: c.id,
    name: c.name,
    date: c.newest,
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
