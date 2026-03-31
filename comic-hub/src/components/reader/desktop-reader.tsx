'use client';

import { useCallback, useEffect, useRef } from 'react';
import { useVirtualizer } from '@tanstack/react-virtual';
import { toast } from 'sonner';
import type { useReader } from '@/hooks/use-reader';
import { ReaderHeader } from './reader-header';
import { StripCard } from './strip-card';
import { StripSkeleton } from './strip-skeleton';
import { DatePickerPopover } from './date-picker-popover';

const HEADER_HEIGHT = 56; // h-14 = 3.5rem = 56px
const STRIP_PADDING = 60; // date label + vertical padding
const FALLBACK_ASPECT = 3; // 3:1 width:height for strips without dimensions
const MAX_CONTENT_WIDTH = 768; // max-w-3xl

interface DesktopReaderProps {
  reader: ReturnType<typeof useReader>;
}

export function DesktopReader({ reader }: DesktopReaderProps) {
  const {
    strips,
    currentIndex,
    setCurrentIndex,
    comicName,
    oldest,
    newest,
    hasOlder,
    hasNewer,
    isLoading,
    loadOlder,
    loadNewer,
    goToDate,
    goToFirst,
    goToLast,
    goToRandom,
    isLoadingRandom,
  } = reader;

  const handleGoToFirst = useCallback(() => {
    const result = goToFirst();
    if (result === 'already') toast.info('Already at the first strip');
  }, [goToFirst]);

  const handleGoToLast = useCallback(() => {
    const result = goToLast();
    if (result === 'already') toast.info('Already at the latest strip');
  }, [goToLast]);

  const scrollContainerRef = useRef<HTMLDivElement>(null);
  const initialScrollDone = useRef(false);

  const virtualizer = useVirtualizer({
    count: strips.length,
    getScrollElement: () => scrollContainerRef.current,
    estimateSize: (index) => {
      const strip = strips[index];
      if (strip.width && strip.height) {
        const contentWidth = Math.min(MAX_CONTENT_WIDTH, window.innerWidth - 32);
        return (contentWidth * strip.height) / strip.width + STRIP_PADDING;
      }
      return MAX_CONTENT_WIDTH / FALLBACK_ASPECT + STRIP_PADDING;
    },
    overscan: 3,
    paddingStart: HEADER_HEIGHT,
    paddingEnd: 32,
  });

  // Scroll to starting strip on initial load
  useEffect(() => {
    if (!initialScrollDone.current && strips.length > 0 && currentIndex >= 0) {
      virtualizer.scrollToIndex(currentIndex, { align: 'center' });
      initialScrollDone.current = true;
    }
  }, [strips.length, currentIndex, virtualizer]);

  // Scroll to strip when currentIndex changes via goToFirst/goToLast/goToDate
  const prevCurrentIndex = useRef(currentIndex);
  useEffect(() => {
    if (initialScrollDone.current && currentIndex !== prevCurrentIndex.current) {
      virtualizer.scrollToIndex(currentIndex, { align: 'center', behavior: 'smooth' });
    }
    prevCurrentIndex.current = currentIndex;
  }, [currentIndex, virtualizer]);

  // Track current strip from scroll position
  const debounceTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  useEffect(() => {
    if (!initialScrollDone.current || strips.length === 0) return;

    const scrollEl = scrollContainerRef.current;
    if (!scrollEl) return;

    const handleScroll = () => {
      if (debounceTimerRef.current) clearTimeout(debounceTimerRef.current);
      debounceTimerRef.current = setTimeout(() => {
        const items = virtualizer.getVirtualItems();
        if (items.length === 0) return;

        const viewportCenter = scrollEl.scrollTop + scrollEl.clientHeight / 2;
        let closestIdx = items[0].index;
        let closestDist = Infinity;
        for (const item of items) {
          const itemCenter = item.start + item.size / 2;
          const dist = Math.abs(itemCenter - viewportCenter);
          if (dist < closestDist) {
            closestDist = dist;
            closestIdx = item.index;
          }
        }
        setCurrentIndex(closestIdx);
      }, 100);
    };

    scrollEl.addEventListener('scroll', handleScroll, { passive: true });
    return () => {
      scrollEl.removeEventListener('scroll', handleScroll);
      if (debounceTimerRef.current) clearTimeout(debounceTimerRef.current);
    };
  }, [strips.length, setCurrentIndex, virtualizer]);

  // Infinite scroll — load more when near boundaries
  useEffect(() => {
    if (!initialScrollDone.current || strips.length === 0) return;

    const items = virtualizer.getVirtualItems();
    if (items.length === 0) return;

    const firstVisible = items[0];
    const lastVisible = items[items.length - 1];

    if (firstVisible.index <= 1 && hasOlder) {
      loadOlder();
    }
    if (lastVisible.index >= strips.length - 2 && hasNewer) {
      loadNewer();
    }
  }, [virtualizer.getVirtualItems(), strips.length, hasOlder, hasNewer, loadOlder, loadNewer]);

  // Keyboard navigation
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return;

      switch (e.key) {
        case 'Home':
          e.preventDefault();
          handleGoToFirst();
          break;
        case 'End':
          e.preventDefault();
          handleGoToLast();
          break;
        case 'r':
        case 'R':
          if (!e.ctrlKey && !e.metaKey) {
            e.preventDefault();
            goToRandom();
          }
          break;
        case 'Escape':
          e.preventDefault();
          window.history.back();
          break;
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [handleGoToFirst, handleGoToLast, goToRandom]);

  return (
    <div ref={scrollContainerRef} className="h-screen overflow-y-auto bg-canvas">
      <ReaderHeader
        comicName={comicName}
        onFirst={handleGoToFirst}
        onLast={handleGoToLast}
        onRandom={goToRandom}
        isLoadingRandom={isLoadingRandom}
        datePicker={
          <DatePickerPopover
            oldest={oldest}
            newest={newest}
            currentDate={strips[currentIndex]?.date ?? null}
            onSelectDate={goToDate}
          />
        }
      />

      <main className="px-4">
        <div className="max-w-3xl mx-auto">
          {isLoading ? (
            <div className="space-y-6 py-4 pt-18">
              <StripSkeleton className="bg-card rounded-lg p-4" />
              <StripSkeleton className="bg-card rounded-lg p-4" />
              <StripSkeleton className="bg-card rounded-lg p-4" />
            </div>
          ) : (
            <div
              style={{
                height: virtualizer.getTotalSize(),
                position: 'relative',
                width: '100%',
              }}
            >
              {virtualizer.getVirtualItems().map((virtualItem) => (
                <div
                  key={strips[virtualItem.index].date}
                  data-index={virtualItem.index}
                  ref={virtualizer.measureElement}
                  style={{
                    position: 'absolute',
                    top: 0,
                    left: 0,
                    width: '100%',
                    transform: `translateY(${virtualItem.start}px)`,
                  }}
                >
                  <StripCard
                    strip={strips[virtualItem.index]}
                    comicName={comicName}
                  />
                </div>
              ))}
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
