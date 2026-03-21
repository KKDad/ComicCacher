'use client';

import { useMemo } from 'react';
import { useRouter } from 'next/navigation';
import { useGetComicsQuery, useGetUserPreferencesQuery } from '@/generated/graphql';
import { usePreferencesStore } from '@/stores/preferences-store';

interface ReadingListComic {
  id: number;
  name: string;
  avatarUrl: string | null;
  lastReadDate: string | null;
  newest: string | null;
  hasUnread: boolean;
}

interface UseReadingListReturn {
  comics: ReadingListComic[];
  previousComic: ReadingListComic | null;
  nextComic: ReadingListComic | null;
  navigateToComic: (comicId: number) => void;
  isLoading: boolean;
}

export function useReadingList(currentComicId: number): UseReadingListReturn {
  const router = useRouter();
  const navMode = usePreferencesStore((s) => s.settings.readerNavMode);

  const { data: comicsData, isLoading: comicsLoading } = useGetComicsQuery(
    { first: 200 },
    { staleTime: 5 * 60 * 1000 },
  );

  const { data: prefsData, isLoading: prefsLoading } = useGetUserPreferencesQuery(
    undefined,
    { staleTime: 5 * 60 * 1000 },
  );

  const comics = useMemo(() => {
    if (!comicsData?.comics?.edges || !prefsData?.preferences) return [];

    const favorites = new Set(prefsData.preferences.favoriteComics ?? []);
    const lastReadMap = new Map<number, string>();
    for (const lr of prefsData.preferences.lastReadDates ?? []) {
      lastReadMap.set(lr.comicId, lr.date);
    }

    const allComics: ReadingListComic[] = comicsData.comics.edges
      .map((edge) => {
        const node = edge.node;
        const lastRead = lastReadMap.get(node.id) ?? null;
        return {
          id: node.id,
          name: node.name,
          avatarUrl: node.avatarUrl ?? null,
          lastReadDate: lastRead,
          newest: node.newest ?? null,
          hasUnread: lastRead !== null && node.newest !== null && lastRead < node.newest,
        };
      })
      .sort((a, b) => a.name.localeCompare(b.name));

    if (navMode === 'favorites') {
      return allComics.filter((c) => favorites.has(c.id));
    }
    return allComics;
  }, [comicsData, prefsData, navMode]);

  const currentIdx = comics.findIndex((c) => c.id === currentComicId);
  const previousComic = currentIdx > 0 ? comics[currentIdx - 1] : null;
  const nextComic = currentIdx >= 0 && currentIdx < comics.length - 1 ? comics[currentIdx + 1] : null;

  const navigateToComic = (comicId: number) => {
    const comic = comics.find((c) => c.id === comicId);
    const dateParam = comic?.lastReadDate ? `?date=${comic.lastReadDate}` : '';
    router.push(`/comics/${comicId}/read${dateParam}`);
  };

  return {
    comics,
    previousComic,
    nextComic,
    navigateToComic,
    isLoading: comicsLoading || prefsLoading,
  };
}
