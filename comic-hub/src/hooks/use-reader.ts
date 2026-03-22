'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import {
  useGetStripWindowQuery,
  useGetRandomStripQuery,
  useUpdateLastReadMutation,
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

const WINDOW_SIZE = 2;

function preloadImage(url: string | null) {
  if (url) {
    const img = new Image();
    img.src = url;
  }
}

export function useReader({ comicId, initialDate, mode }: UseReaderOptions): UseReaderReturn {
  const [centerDate, setCenterDate] = useState<string | undefined>(initialDate);
  const [strips, setStrips] = useState<Strip[]>([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [hasOlder, setHasOlder] = useState(true);
  const [hasNewer, setHasNewer] = useState(true);
  const [randomComicId, setRandomComicId] = useState<number | null>(null);

  const queryClient = useQueryClient();

  // Main strip window query
  const { data: windowData, isLoading: windowLoading } = useGetStripWindowQuery(
    {
      comicId,
      center: centerDate ?? '',
      before: WINDOW_SIZE,
      after: WINDOW_SIZE,
    },
    {
      enabled: !!centerDate,
      staleTime: 5 * 60 * 1000,
    },
  );

  // If no initial date, fetch the comic to get its newest date
  const needsLatest = !initialDate && !centerDate;
  const { data: latestData } = useGetStripWindowQuery(
    {
      comicId,
      center: '9999-12-31', // Far future — BE clamps to newest
      before: WINDOW_SIZE,
      after: 0,
    },
    {
      enabled: needsLatest,
      staleTime: 5 * 60 * 1000,
    },
  );

  // Set center date from latest data when no initial date provided
  useEffect(() => {
    if (needsLatest && latestData?.comic?.stripWindow?.length) {
      const latestStrips = latestData.comic.stripWindow;
      const last = latestStrips[latestStrips.length - 1];
      setCenterDate(last.date);
    }
  }, [needsLatest, latestData]);

  // Random strip query
  const [fetchRandom, setFetchRandom] = useState(false);
  const { data: randomData, isLoading: isLoadingRandom } = useGetRandomStripQuery(
    { comicId: randomComicId ?? comicId },
    {
      enabled: fetchRandom,
      staleTime: 0, // Always fresh
    },
  );

  // Process strip window data
  useEffect(() => {
    if (!windowData?.comic) return;

    const comic = windowData.comic;
    const newStrips: Strip[] = comic.stripWindow.map((s) => ({
      date: s.date,
      available: s.available,
      imageUrl: s.imageUrl ?? null,
      width: s.width ?? null,
      height: s.height ?? null,
    }));

    // Empty windows are expected at date-range boundaries — nothing to merge
    if (newStrips.length === 0) return;

    setStrips((prev) => {
      if (prev.length === 0) return newStrips;

      // Merge new strips into existing array, maintaining chronological order
      const dateSet = new Set(prev.map((s) => s.date));
      const toAdd = newStrips.filter((s) => !dateSet.has(s.date));

      if (toAdd.length === 0) return prev;

      const merged = [...prev, ...toAdd].sort(
        (a, b) => new Date(a.date).getTime() - new Date(b.date).getTime(),
      );
      return merged;
    });

    // Determine boundary flags
    const oldest = comic.oldest;
    const newest = comic.newest;
    if (oldest && newStrips.length > 0) {
      setHasOlder(newStrips[0].date > oldest);
    }
    if (newest && newStrips.length > 0) {
      setHasNewer(newStrips[newStrips.length - 1].date < newest);
    }
  }, [windowData]);

  // Set initial currentIndex to the center strip
  const initialIndexSet = useRef(false);
  useEffect(() => {
    if (!initialIndexSet.current && strips.length > 0 && centerDate) {
      const idx = strips.findIndex((s) => s.date === centerDate);
      if (idx >= 0) {
        setCurrentIndex(idx);
        initialIndexSet.current = true;
      }
    }
  }, [strips, centerDate]);

  // Preload adjacent strip images
  useEffect(() => {
    if (strips.length === 0) return;
    const prevStrip = strips[currentIndex - 1];
    const nextStrip = strips[currentIndex + 1];
    if (prevStrip) preloadImage(prevStrip.imageUrl);
    if (nextStrip) preloadImage(nextStrip.imageUrl);
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

  useEffect(() => {
    const strip = strips[currentIndex];
    if (!strip || !strip.available || !strip.imageUrl) return;

    const key = `${comicId}:${strip.date}`;
    if (lastReadRef.current === key) return;

    const doUpdate = () => {
      lastReadRef.current = key;
      updateLastRead.mutate({ comicId, date: strip.date });
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
  }, [strips, currentIndex, comicId, mode, updateLastRead]);

  // Navigation functions — declared before effects that use them
  const goToDate = useCallback((date: string) => {
    setStrips([]);
    initialIndexSet.current = false;
    setCenterDate(date);
  }, []);

  // Handle random strip result
  useEffect(() => {
    if (randomData?.randomStrip && fetchRandom) {
      setFetchRandom(false);
      goToDate(randomData.randomStrip.date);
    }
  }, [randomData, fetchRandom, goToDate]);

  const loadOlder = useCallback(() => {
    if (strips.length === 0 || !hasOlder) return;
    const oldestStrip = strips[0];
    setCenterDate(oldestStrip.date);
  }, [strips, hasOlder]);

  const loadNewer = useCallback(() => {
    if (strips.length === 0 || !hasNewer) return;
    const newestStrip = strips[strips.length - 1];
    setCenterDate(newestStrip.date);
  }, [strips, hasNewer]);

  const goToFirst = useCallback((): 'already' | 'scrolled' | 'loading' => {
    const oldestDate = windowData?.comic?.oldest;
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
  }, [windowData, strips, currentIndex, goToDate]);

  const goToLast = useCallback((): 'already' | 'scrolled' | 'loading' => {
    const newestDate = windowData?.comic?.newest;
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
  }, [windowData, strips, currentIndex, goToDate]);

  const goToRandom = useCallback(() => {
    setRandomComicId(null);
    setFetchRandom(true);
    // Invalidate to force refetch
    queryClient.invalidateQueries({ queryKey: ['GetRandomStrip'] });
  }, [queryClient]);

  const goNewer = useCallback(() => {
    if (currentIndex < strips.length - 1) {
      setCurrentIndex(currentIndex + 1);
    } else if (hasNewer) {
      loadNewer();
    }
  }, [currentIndex, strips.length, hasNewer, loadNewer]);

  const goOlder = useCallback(() => {
    if (currentIndex > 0) {
      setCurrentIndex(currentIndex - 1);
    } else if (hasOlder) {
      loadOlder();
    }
  }, [currentIndex, hasOlder, loadOlder]);

  const comicName = windowData?.comic?.name ?? latestData?.comic?.name ?? '';
  const oldest = windowData?.comic?.oldest ?? latestData?.comic?.oldest ?? null;
  const newest = windowData?.comic?.newest ?? latestData?.comic?.newest ?? null;
  const avatarUrl = windowData?.comic?.avatarUrl ?? latestData?.comic?.avatarUrl ?? null;
  const isLoading = windowLoading || (needsLatest && !centerDate);

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
      comicName,
      oldest,
      newest,
      avatarUrl,
      hasOlder,
      hasNewer,
      isLoading,
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
