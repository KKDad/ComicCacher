'use client';

import { useCallback, useEffect, useMemo } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useQueryClient } from '@tanstack/react-query';
import {
  useGetComicsForDateQuery,
  useGetUserPreferencesQuery,
} from '@/generated/graphql';
import { usePreferencesStore } from '@/stores/preferences-store';
import { shiftIsoDate, todayIsoDate } from '@/lib/date-utils';
import { compareByName } from '@/lib/sort';
import type { LightboxItem } from '@/components/grid-reader/lightbox';

export interface GridComic {
  id: number;
  name: string;
  avatarUrl: string | null;
  oldest: string | null;
  newest: string | null;
  strip: {
    date: string;
    available: boolean;
    imageUrl: string | null;
    width: number | null;
    height: number | null;
    transcript: string | null;
  } | null;
}

interface UseGridReaderOptions {
  initialDate?: string;
}

interface UseGridReaderReturn {
  date: string;
  comics: GridComic[];
  isLoading: boolean;
  goToDate: (date: string) => void;
  goToNextDate: () => void;
  goToPreviousDate: () => void;
  goToToday: () => void;
}

/** The comics' strips in the shape the lightbox shows. */
export function toLightboxItems(comics: GridComic[]): LightboxItem[] {
  return comics.map((c) => ({ title: c.name, date: c.strip?.date ?? '', imageUrl: c.strip?.imageUrl ?? null }));
}

export function useGridReader({ initialDate }: UseGridReaderOptions = {}): UseGridReaderReturn {
  const router = useRouter();
  const searchParams = useSearchParams();
  const queryClient = useQueryClient();
  const navMode = usePreferencesStore((s) => s.settings.readerNavMode);

  const date = searchParams.get('date') ?? initialDate ?? todayIsoDate();

  const { data: comicsData, isLoading: comicsLoading } = useGetComicsForDateQuery(
    { first: 200, date },
    { staleTime: 5 * 60 * 1000 },
  );

  const { data: prefsData, isLoading: prefsLoading } = useGetUserPreferencesQuery(
    undefined,
    { staleTime: 5 * 60 * 1000 },
  );

  const comics = useMemo((): GridComic[] => {
    if (!comicsData?.comics?.edges || !prefsData?.preferences) return [];

    const favorites = new Set(prefsData.preferences.favoriteComics ?? []);

    const allComics: GridComic[] = comicsData.comics.edges
      .map((edge) => {
        const node = edge.node;
        return {
          id: node.id,
          name: node.name,
          avatarUrl: node.avatarUrl ?? null,
          oldest: node.oldest ?? null,
          newest: node.newest ?? null,
          strip: node.strip
            ? {
                date: node.strip.date,
                available: node.strip.available,
                imageUrl: node.strip.imageUrl ?? null,
                width: node.strip.width ?? null,
                height: node.strip.height ?? null,
                transcript: node.strip.transcript ?? null,
              }
            : null,
        };
      })
      .sort(compareByName);

    if (navMode === 'favorites') {
      return allComics.filter((c) => favorites.has(c.id));
    }
    return allComics;
  }, [comicsData, prefsData, navMode]);

  // Prefetch adjacent dates for instant Left/Right navigation
  useEffect(() => {
    const prevDate = shiftIsoDate(date, -1);
    const nextDate = shiftIsoDate(date, 1);

    const prefetchDate = (d: string) => {
      queryClient.prefetchQuery({
        queryKey: useGetComicsForDateQuery.getKey({ first: 200, date: d }),
        queryFn: () => useGetComicsForDateQuery.fetcher({ first: 200, date: d })(),
        staleTime: 5 * 60 * 1000,
      });
    };

    prefetchDate(prevDate);
    prefetchDate(nextDate);
  }, [date, queryClient]);

  const goToDate = useCallback(
    (newDate: string) => {
      router.replace(`/read?date=${newDate}`, { scroll: false });
    },
    [router],
  );

  const goToNextDate = useCallback(() => {
    goToDate(shiftIsoDate(date, 1));
  }, [date, goToDate]);

  const goToPreviousDate = useCallback(() => {
    goToDate(shiftIsoDate(date, -1));
  }, [date, goToDate]);

  const goToToday = useCallback(() => {
    goToDate(todayIsoDate());
  }, [goToDate]);

  return useMemo(
    () => ({
      date,
      comics,
      isLoading: comicsLoading || prefsLoading,
      goToDate,
      goToNextDate,
      goToPreviousDate,
      goToToday,
    }),
    [date, comics, comicsLoading, prefsLoading, goToDate, goToNextDate, goToPreviousDate, goToToday],
  );
}
