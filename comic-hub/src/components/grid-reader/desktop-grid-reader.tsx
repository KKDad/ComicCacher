'use client';

import { useCallback, useEffect, useRef } from 'react';
import { useRouter } from 'next/navigation';
import { useVirtualizer } from '@tanstack/react-virtual';
import { useQueryClient } from '@tanstack/react-query';
import { useGetRandomStripQuery } from '@/generated/graphql';
import { GridHeader } from './grid-header';
import { GridStripCard } from './grid-strip-card';
import { Lightbox } from './lightbox';
import { StripSkeleton } from '@/components/reader/strip-skeleton';
import { useLightbox } from '@/hooks/use-lightbox';
import type { useGridReader } from '@/hooks/use-grid-reader';
import { useState } from 'react';

const HEADER_HEIGHT = 56;
const CARD_PADDING = 80; // avatar row + transcript toggle + card margins
const FALLBACK_ASPECT = 3;
const MAX_CONTENT_WIDTH = 768;

interface DesktopGridReaderProps {
  reader: ReturnType<typeof useGridReader>;
}

export function DesktopGridReader({ reader }: DesktopGridReaderProps) {
  const { date, comics, isLoading, goToDate, goToNextDate, goToPreviousDate, goToToday } = reader;
  const router = useRouter();
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

  const scrollContainerRef = useRef<HTMLDivElement>(null);

  const virtualizer = useVirtualizer({
    count: comics.length,
    getScrollElement: () => scrollContainerRef.current,
    estimateSize: (index) => {
      const strip = comics[index]?.strip;
      if (strip?.width && strip.height) {
        const contentWidth = Math.min(MAX_CONTENT_WIDTH, window.innerWidth - 32);
        return (contentWidth * strip.height) / strip.width + CARD_PADDING;
      }
      return MAX_CONTENT_WIDTH / FALLBACK_ASPECT + CARD_PADDING;
    },
    overscan: 3,
    paddingStart: HEADER_HEIGHT,
    paddingEnd: 32,
  });

  // Keyboard navigation (bubble phase — lightbox captures first if open)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return;

      switch (e.key) {
        case 'ArrowLeft':
          e.preventDefault();
          goToPreviousDate();
          break;
        case 'ArrowRight':
          e.preventDefault();
          goToNextDate();
          break;
        case 'Home':
          e.preventDefault();
          goToDate('1900-01-01'); // BE clamps to oldest
          break;
        case 'End':
          e.preventDefault();
          goToToday();
          break;
        case 'Escape':
          e.preventDefault();
          router.back();
          break;
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [goToPreviousDate, goToNextDate, goToDate, goToToday, router]);

  // Scroll to top when date changes
  useEffect(() => {
    scrollContainerRef.current?.scrollTo({ top: 0 });
  }, [date]);

  return (
    <div ref={scrollContainerRef} className="h-screen overflow-y-auto bg-canvas">
      <GridHeader
        date={date}
        onPreviousDate={goToPreviousDate}
        onNextDate={goToNextDate}
        onSelectDate={goToDate}
        onToday={goToToday}
      />

      <main className="px-4">
        <div className="max-w-3xl mx-auto">
          {isLoading ? (
            <div className="space-y-6 py-4 pt-18">
              <StripSkeleton className="bg-card rounded-lg p-4" />
              <StripSkeleton className="bg-card rounded-lg p-4" />
              <StripSkeleton className="bg-card rounded-lg p-4" />
            </div>
          ) : comics.length === 0 ? (
            <div className="flex items-center justify-center h-[50vh] text-ink-muted text-sm">
              No comics to display. Check your favorites or subscription settings.
            </div>
          ) : (
            <div
              style={{
                height: virtualizer.getTotalSize(),
                position: 'relative',
                width: '100%',
              }}
            >
              {virtualizer.getVirtualItems().map((virtualItem) => {
                const comic = comics[virtualItem.index];
                return (
                  <div
                    key={comic.id}
                    data-index={virtualItem.index}
                    ref={virtualizer.measureElement}
                    style={{
                      position: 'absolute',
                      top: 0,
                      left: 0,
                      width: '100%',
                      transform: `translateY(${virtualItem.start}px)`,
                    }}
                    className="py-2"
                  >
                    <GridStripCard
                      comic={comic}
                      date={date}
                      onImageClick={() => lightbox.open(virtualItem.index)}
                      onRandom={handleRandom}
                    />
                  </div>
                );
              })}
            </div>
          )}
        </div>
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
