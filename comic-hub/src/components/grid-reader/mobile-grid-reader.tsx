'use client';

import { useCallback, useEffect, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useGetRandomStripQuery } from '@/generated/graphql';
import { GridHeader } from './grid-header';
import { GridStripCard } from './grid-strip-card';
import { Lightbox } from './lightbox';
import { StripSkeleton } from '@/components/reader/strip-skeleton';
import { useLightbox } from '@/hooks/use-lightbox';
import { useSwipe } from '@/hooks/use-swipe';
import type { useGridReader } from '@/hooks/use-grid-reader';

interface MobileGridReaderProps {
  reader: ReturnType<typeof useGridReader>;
}

export function MobileGridReader({ reader }: MobileGridReaderProps) {
  const { date, comics, isLoading, goToDate, goToNextDate, goToPreviousDate, goToToday } = reader;
  const lightbox = useLightbox(comics.length);

  // Random strip handling
  const [randomComicId, setRandomComicId] = useState<number | null>(null);
  const [fetchRandom, setFetchRandom] = useState(false);
  const queryClient = useQueryClient();

  const { data: randomData } = useGetRandomStripQuery(
    { comicId: randomComicId ?? 0 },
    { enabled: fetchRandom && randomComicId !== null, staleTime: 0 },
  );

  useEffect(() => {
    if (randomData?.randomStrip && fetchRandom) {
      setFetchRandom(false);
      goToDate(randomData.randomStrip.date);
    }
  }, [randomData, fetchRandom, goToDate]);

  const handleRandom = useCallback(
    (comicId: number) => {
      setRandomComicId(comicId);
      setFetchRandom(true);
      queryClient.invalidateQueries({ queryKey: ['GetRandomStrip'] });
    },
    [queryClient],
  );

  // Horizontal swipe for date navigation
  const swipeHandlers = useSwipe({
    onSwipeLeft: goToNextDate,
    onSwipeRight: goToPreviousDate,
  });

  return (
    <div className="h-screen overflow-y-auto bg-canvas" {...swipeHandlers}>
      <GridHeader
        date={date}
        onPreviousDate={goToPreviousDate}
        onNextDate={goToNextDate}
        onSelectDate={goToDate}
        onToday={goToToday}
      />

      <main className="px-3 pt-16 pb-6 space-y-3">
        {isLoading ? (
          <div className="space-y-4">
            <StripSkeleton className="bg-card rounded-lg p-4" />
            <StripSkeleton className="bg-card rounded-lg p-4" />
          </div>
        ) : comics.length === 0 ? (
          <div className="flex items-center justify-center h-[50vh] text-ink-muted text-sm">
            No comics to display.
          </div>
        ) : (
          comics.map((comic, index) => (
            <GridStripCard
              key={comic.id}
              comic={comic}
              date={date}
              onImageClick={() => lightbox.open(index)}
              onRandom={handleRandom}
            />
          ))
        )}
      </main>

      {lightbox.isOpen && (
        <Lightbox
          comics={comics}
          currentIndex={lightbox.currentIndex}
          onClose={lightbox.close}
          onNext={lightbox.next}
          onPrevious={lightbox.previous}
        />
      )}
    </div>
  );
}
