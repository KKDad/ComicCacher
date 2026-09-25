'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useQueryClient, type InfiniteData } from '@tanstack/react-query';
import { toast } from 'sonner';
import {
  useGetStripWindowQuery,
  useInfiniteGetStripWindowQuery,
  useGetRandomStripQuery,
  useUpdateLastReadMutation,
  type GetStripWindowQuery,
} from '@/generated/graphql';

export interface Strip {
  date: string;
  available: boolean;
  imageUrl: string | null;
  width: number | null;
  height: number | null;
}

interface UseReaderOptions {
  comicId: number;
  initialDate?: string;
  mode: 'scroll' | 'snap';
}

interface UseReaderReturn {
  strips: Strip[];
  currentIndex: number;
  setCurrentIndex: (index: number) => void;
  comicName: string;
  oldest: string | null;
  newest: string | null;
  avatarUrl: string | null;
  hasOlder: boolean;
  hasNewer: boolean;
  isLoading: boolean;
  isFetchingOlder: boolean;
  isFetchingNewer: boolean;
  loadOlder: () => void;
  loadNewer: () => void;
  goToDate: (date: string) => void;
  goToFirst: () => 'already' | 'scrolled' | 'loading';
  goToLast: () => 'already' | 'scrolled' | 'loading';
  goToRandom: () => void;
  goNewer: () => void;
  goOlder: () => void;
  isLoadingRandom: boolean;
}

/** Strips either side of the date the reader opens on. */
const INITIAL_SPAN = 10;
/** Strips per older/newer page. The backend caps `stripWindow` before/after at 20. */
const PAGE_SIZE = 20;
/** Strips before the newest shown when the reader opens without a date. */
const LATEST_SPAN = 2;

type StripPage = GetStripWindowQuery;

function preloadImage(url: string | null) {
  if (url) {
    const img = new Image();
    img.src = url;
  }
}

/**
 * Flattens the pages into one chronological list. Every page repeats the strip it is
 * centred on (the edge strip of its neighbour), so dates are de-duplicated.
 */
function flattenStrips(pages: StripPage[] | undefined): Strip[] {
  const seen = new Set<string>();
  const strips: Strip[] = [];
  for (const page of pages ?? []) {
    for (const s of page.comic?.stripWindow ?? []) {
      if (seen.has(s.date)) continue;
      seen.add(s.date);
      strips.push({
        date: s.date,
        available: s.available,
        imageUrl: s.imageUrl ?? null,
        width: s.width ?? null,
        height: s.height ?? null,
      });
    }
  }
  return strips;
}

export function useReader({ comicId, initialDate, mode }: UseReaderOptions): UseReaderReturn {
  // The date the strip list is built around (set by goToDate). Changing it starts a
  // fresh list. Without one, the list is built around the newest strip.
  const [chosenAnchor, setChosenAnchor] = useState<string | undefined>(initialDate);
  // The strip being read. Tracked by date so strips loading above it don't move it.
  const [chosenDate, setChosenDate] = useState<string | undefined>(initialDate);
  const [isLoadingRandom, setIsLoadingRandom] = useState(false);

  const queryClient = useQueryClient();

  // If no date was given, fetch the comic to find its newest date. The window's last entry
  // is the far-future centre itself (returned as unavailable), so read `newest` instead.
  const needsLatest = !chosenAnchor;
  const { data: latestData } = useGetStripWindowQuery(
    {
      comicId,
      center: '9999-12-31',
      before: LATEST_SPAN,
      after: 0,
    },
    {
      enabled: needsLatest,
      staleTime: 5 * 60 * 1000,
    },
  );
  const latestDate: string | undefined = latestData?.comic?.newest ?? undefined;
  const anchorDate = chosenAnchor ?? latestDate;
  const currentDate = chosenDate ?? anchorDate;

  // Strip list: one page around the anchor, extended a page at a time in either direction
  const {
    data: stripData,
    isLoading: stripsLoading,
    isFetching,
    isFetchingPreviousPage,
    isFetchingNextPage,
    hasPreviousPage,
    hasNextPage,
    fetchPreviousPage,
    fetchNextPage,
  } = useInfiniteGetStripWindowQuery(
    {
      comicId,
      center: anchorDate ?? '',
      before: INITIAL_SPAN,
      after: INITIAL_SPAN,
    },
    {
      enabled: !!anchorDate,
      staleTime: 5 * 60 * 1000,
      initialPageParam: {},
      getPreviousPageParam: (firstPage) => {
        const comic = firstPage.comic;
        const first = comic?.stripWindow[0]?.date;
        if (!first || !comic?.oldest || first <= comic.oldest) return undefined;
        return { center: first, before: PAGE_SIZE, after: 0 };
      },
      getNextPageParam: (lastPage) => {
        const comic = lastPage.comic;
        const last = comic?.stripWindow.at(-1)?.date;
        if (!last || !comic?.newest || last >= comic.newest) return undefined;
        return { center: last, before: 0, after: PAGE_SIZE };
      },
    },
  );

  const strips = useMemo(() => flattenStrips(stripData?.pages), [stripData]);
  const comicMeta = stripData?.pages[0]?.comic;

  const foundIndex = strips.findIndex((s) => s.date === currentDate);
  const currentIndex = foundIndex >= 0 ? foundIndex : 0;

  const setCurrentIndex = useCallback(
    (index: number) => {
      const strip = strips[index];
      if (strip) setChosenDate(strip.date);
    },
    [strips],
  );

  // Preload adjacent strip images
  useEffect(() => {
    if (strips.length === 0) return;
    preloadImage(strips[currentIndex - 1]?.imageUrl ?? null);
    preloadImage(strips[currentIndex + 1]?.imageUrl ?? null);
  }, [strips, currentIndex]);

  // Last-read tracking
  const updateLastRead = useUpdateLastReadMutation({
    onSuccess: (data) => {
      if (data.updateLastRead.errors.length === 0) {
        queryClient.invalidateQueries({ queryKey: ['GetUserPreferences'] });
      }
    },
  });

  const lastReadRef = useRef<string | null>(null);
  const lastReadTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const currentStrip = foundIndex >= 0 ? strips[foundIndex] : undefined;
  useEffect(() => {
    if (!currentStrip || !currentStrip.available || !currentStrip.imageUrl) return;

    const key = `${comicId}:${currentStrip.date}`;
    if (lastReadRef.current === key) return;

    const doUpdate = () => {
      lastReadRef.current = key;
      updateLastRead.mutate({ comicId, date: currentStrip.date });
    };

    if (mode === 'snap') {
      doUpdate();
    } else {
      // Debounce 1s for scroll mode
      if (lastReadTimerRef.current) clearTimeout(lastReadTimerRef.current);
      lastReadTimerRef.current = setTimeout(doUpdate, 1000);
    }

    return () => {
      if (lastReadTimerRef.current) clearTimeout(lastReadTimerRef.current);
    };
  }, [currentStrip, comicId, mode, updateLastRead]);

  // Navigation functions — declared before effects that use them
  const goToDate = useCallback((date: string) => {
    setChosenAnchor(date);
    setChosenDate(date);
  }, []);

  // One page request at a time: TanStack Query warns that overlapping fetches of an
  // infinite query can overwrite each other.
  const loadOlder = useCallback(() => {
    if (hasPreviousPage && !isFetching) fetchPreviousPage();
  }, [hasPreviousPage, isFetching, fetchPreviousPage]);

  const loadNewer = useCallback(() => {
    if (hasNextPage && !isFetching) fetchNextPage();
  }, [hasNextPage, isFetching, fetchNextPage]);

  const goToFirst = useCallback((): 'already' | 'scrolled' | 'loading' => {
    const oldestDate = comicMeta?.oldest;
    if (!oldestDate) return 'loading';

    const firstIdx = strips.findIndex((s) => s.date === oldestDate);

    // Already at or near the first strip (scroll centering may land 1-2 strips off)
    if (firstIdx >= 0 && currentIndex <= firstIdx + 2) return 'already';

    // First strip is loaded but not in view — scroll to it
    if (firstIdx >= 0) {
      setCurrentIndex(firstIdx);
      return 'scrolled';
    }

    // Not loaded — fetch it
    goToDate(oldestDate);
    return 'loading';
  }, [comicMeta, strips, currentIndex, setCurrentIndex, goToDate]);

  const goToLast = useCallback((): 'already' | 'scrolled' | 'loading' => {
    const newestDate = comicMeta?.newest;
    if (!newestDate) return 'loading';

    const lastIdx = strips.findIndex((s) => s.date === newestDate);

    // Already at or near the latest strip (scroll centering may land 1-2 strips off)
    if (lastIdx >= 0 && currentIndex >= lastIdx - 2) return 'already';

    // Latest strip is loaded but not in view — scroll to it
    if (lastIdx >= 0) {
      setCurrentIndex(lastIdx);
      return 'scrolled';
    }

    // Not loaded — fetch it
    goToDate(newestDate);
    return 'loading';
  }, [comicMeta, strips, currentIndex, setCurrentIndex, goToDate]);

  const goToRandom = useCallback(async () => {
    const variables = { comicId };
    setIsLoadingRandom(true);
    try {
      const data = await queryClient.fetchQuery({
        queryKey: useGetRandomStripQuery.getKey(variables),
        queryFn: useGetRandomStripQuery.fetcher(variables),
        staleTime: 0, // A new random strip every time
      });
      if (data.randomStrip) goToDate(data.randomStrip.date);
    } catch {
      toast.error('Could not load a random strip');
    } finally {
      setIsLoadingRandom(false);
    }
  }, [comicId, queryClient, goToDate]);

  // Step one strip from `from` once a page has loaded past the edge
  const stepAfterLoad = useCallback(
    (result: { data?: InfiniteData<StripPage> }, from: string | undefined, step: -1 | 1) => {
      const loaded = flattenStrips(result.data?.pages);
      const idx = loaded.findIndex((s) => s.date === from);
      const target = idx >= 0 ? loaded[idx + step] : undefined;
      if (target) setChosenDate(target.date);
    },
    [],
  );

  const goNewer = useCallback(() => {
    if (currentIndex < strips.length - 1) {
      setCurrentIndex(currentIndex + 1);
    } else if (hasNextPage && !isFetching) {
      fetchNextPage().then((result) => stepAfterLoad(result, currentDate, 1));
    }
  }, [currentIndex, strips.length, setCurrentIndex, hasNextPage, isFetching, fetchNextPage, stepAfterLoad, currentDate]);

  const goOlder = useCallback(() => {
    if (currentIndex > 0) {
      setCurrentIndex(currentIndex - 1);
    } else if (hasPreviousPage && !isFetching) {
      fetchPreviousPage().then((result) => stepAfterLoad(result, currentDate, -1));
    }
  }, [currentIndex, setCurrentIndex, hasPreviousPage, isFetching, fetchPreviousPage, stepAfterLoad, currentDate]);

  const comicName = comicMeta?.name ?? latestData?.comic?.name ?? '';
  const oldest = comicMeta?.oldest ?? latestData?.comic?.oldest ?? null;
  const newest = comicMeta?.newest ?? latestData?.comic?.newest ?? null;
  const avatarUrl = comicMeta?.avatarUrl ?? latestData?.comic?.avatarUrl ?? null;
  const isLoading = stripsLoading || !anchorDate;
  const hasOlder = !!hasPreviousPage;
  const hasNewer = !!hasNextPage;

  return useMemo(
    () => ({
      strips,
      currentIndex,
      setCurrentIndex,
      comicName,
      oldest,
      newest,
      avatarUrl,
      hasOlder,
      hasNewer,
      isLoading,
      isFetchingOlder: isFetchingPreviousPage,
      isFetchingNewer: isFetchingNextPage,
      loadOlder,
      loadNewer,
      goToDate,
      goToFirst,
      goToLast,
      goToRandom,
      goNewer,
      goOlder,
      isLoadingRandom,
    }),
    [
      strips,
      currentIndex,
      setCurrentIndex,
      comicName,
      oldest,
      newest,
      avatarUrl,
      hasOlder,
      hasNewer,
      isLoading,
      isFetchingPreviousPage,
      isFetchingNextPage,
      loadOlder,
      loadNewer,
      goToDate,
      goToFirst,
      goToLast,
      goToRandom,
      goNewer,
      goOlder,
      isLoadingRandom,
    ],
  );
}
