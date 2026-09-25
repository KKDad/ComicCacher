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
    isFetchingOlder,
    isFetchingNewer,
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
  // Date of the strip the view was last scrolled to (or scrolled onto by the reader)
  const scrolledToDate = useRef<string | null>(null);

  const virtualizer = useVirtualizer({
    count: strips.length,
    getScrollElement: () => scrollContainerRef.current,
    // Key by date so measured heights follow their strip when older strips are prepended
    getItemKey: (index) => strips[index].date,
    // Keep the strip in view in place when strips are added above it
    anchorTo: 'end',
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

  // Scroll to the current strip on initial load and after goToFirst/goToLast/goToDate.
  // Keyed on the date, not the index: prepending older strips shifts the index but
  // not the strip being read.
  const currentDate = strips[currentIndex]?.date ?? null;
  useEffect(() => {
    if (!currentDate || currentDate === scrolledToDate.current) return;
    const isInitial = scrolledToDate.current === null;
    virtualizer.scrollToIndex(currentIndex, {
      align: 'center',
      behavior: isInitial ? 'auto' : 'smooth',
    });
    scrolledToDate.current = currentDate;
  }, [currentDate, currentIndex, virtualizer]);

  // Track current strip from scroll position
  const debounceTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  useEffect(() => {
    if (scrolledToDate.current === null || strips.length === 0) return;

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
        // The user is already looking at this strip; don't scroll to it again
        scrolledToDate.current = strips[closestIdx]?.date ?? scrolledToDate.current;
        setCurrentIndex(closestIdx);
      }, 100);
    };

    scrollEl.addEventListener('scroll', handleScroll, { passive: true });
    return () => {
      scrollEl.removeEventListener('scroll', handleScroll);
      if (debounceTimerRef.current) clearTimeout(debounceTimerRef.current);
    };
  }, [strips, setCurrentIndex, virtualizer]);

  // Infinite scroll, following TanStack Virtual's infinite-scroll example: load a page
  // when the first or last rendered strip is the edge of the list, one request at a time.
  // The anchoring above keeps the view still when older strips arrive, so the first
  // rendered strip moves away from the edge and this doesn't fire again.
  const virtualItems = virtualizer.getVirtualItems();
  const firstRenderedIndex = virtualItems[0]?.index;
  const lastRenderedIndex = virtualItems.at(-1)?.index;
  const isFetchingPage = isFetchingOlder || isFetchingNewer;
  useEffect(() => {
    if (scrolledToDate.current === null || isFetchingPage) return;
    if (firstRenderedIndex === undefined || lastRenderedIndex === undefined) return;

    if (firstRenderedIndex === 0 && hasOlder) {
      loadOlder();
    } else if (lastRenderedIndex >= strips.length - 1 && hasNewer) {
      loadNewer();
    }
  }, [firstRenderedIndex, lastRenderedIndex, strips.length, hasOlder, hasNewer, isFetchingPage, loadOlder, loadNewer]);

  // Keyboard navigation
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Popovers, menus and the calendar mark the keys they handle (e.g. the
      // Escape that closes them); modifier combos belong to the browser.
      if (e.defaultPrevented || e.altKey || e.ctrlKey || e.metaKey) return;
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
          e.preventDefault();
          goToRandom();
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
              {virtualItems.map((virtualItem) => (
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
