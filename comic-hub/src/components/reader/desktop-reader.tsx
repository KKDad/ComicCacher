'use client';

import { useCallback, useEffect, useMemo, useRef } from 'react';
import { useVirtualizer } from '@tanstack/react-virtual';
import { toast } from 'sonner';
import type { useReader } from '@/hooks/use-reader';
import { ReaderHeader } from './reader-header';
import { StripCard } from './strip-card';
import { StripSkeleton } from './strip-skeleton';
import { DatePickerPopover } from './date-picker-popover';
import { FavoriteButton } from './favorite-button';
import { Lightbox } from '@/components/grid-reader/lightbox';
import { useLightbox } from '@/hooks/use-lightbox';
import { useGoBack } from '@/lib/navigation-history';
import { useNewestFirst } from '@/hooks/use-newest-first';

const HEADER_HEIGHT = 56; // h-14 = 3.5rem = 56px
const STRIP_PADDING = 80; // date label, mat and vertical padding
const FALLBACK_ASPECT = 3; // 3:1 width:height for strips without dimensions
const MAX_CONTENT_WIDTH = 768; // max-w-3xl

interface DesktopReaderProps {
  comicId: number;
  reader: ReturnType<typeof useReader>;
}

export function DesktopReader({ comicId, reader }: DesktopReaderProps) {
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
    goOlder,
    goNewer,
    isLoadingRandom,
  } = reader;

  const goBack = useGoBack('/comics');

  // The reader keeps strips oldest to newest. With "Newest first" the list shows them
  // reversed, so list positions (virtualizer indexes, top and bottom) map through toList.
  const newestFirst = useNewestFirst();
  const listStrips = useMemo(() => (newestFirst ? [...strips].reverse() : strips), [strips, newestFirst]);
  // Converts between reader and list indexes (the mapping is its own inverse)
  const toList = useCallback(
    (index: number) => (newestFirst ? strips.length - 1 - index : index),
    [newestFirst, strips.length],
  );
  const currentListIndex = toList(currentIndex);

  // Fullscreen view: steps through the strips that have an image
  const viewable = useMemo(() => strips.filter((s) => s.available && s.imageUrl), [strips]);
  const lightboxItems = useMemo(
    () => viewable.map((s) => ({ title: comicName, date: s.date, imageUrl: s.imageUrl, width: s.width, height: s.height })),
    [viewable, comicName],
  );
  const lightbox = useLightbox(viewable.length);
  const { open: openLightbox, close: closeLightbox, isOpen: isLightboxOpen } = lightbox;

  const openFullscreen = useCallback(
    (date: string | undefined) => {
      const idx = viewable.findIndex((s) => s.date === date);
      if (idx >= 0) openLightbox(idx);
    },
    [viewable, openLightbox],
  );

  // Closing (button, backdrop or Escape) returns the reader to the strip the lightbox ended on
  const lightboxDate = viewable[lightbox.currentIndex]?.date;
  const wasLightboxOpen = useRef(false);
  useEffect(() => {
    if (wasLightboxOpen.current && !isLightboxOpen) {
      const idx = strips.findIndex((s) => s.date === lightboxDate);
      if (idx >= 0) setCurrentIndex(idx);
    }
    wasLightboxOpen.current = isLightboxOpen;
  }, [isLightboxOpen, lightboxDate, strips, setCurrentIndex]);

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

  // TanStack Virtual returns functions the React Compiler can't memoize safely, so it skips this component
  // eslint-disable-next-line react-hooks/incompatible-library -- no compiler-compatible virtualizer API yet
  const virtualizer = useVirtualizer({
    count: listStrips.length,
    getScrollElement: () => scrollContainerRef.current,
    // Key by date so measured heights follow their strip when strips are added above it
    getItemKey: (index) => listStrips[index].date,
    // Keep the strip in view in place when strips are added above it
    anchorTo: 'end',
    estimateSize: (index) => {
      const strip = listStrips[index];
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
  // Keyed on the date, not the index: adding strips above shifts the index but
  // not the strip being read.
  const currentDate = strips[currentIndex]?.date ?? null;
  useEffect(() => {
    if (!currentDate || currentDate === scrolledToDate.current) return;
    const isInitial = scrolledToDate.current === null;
    virtualizer.scrollToIndex(currentListIndex, {
      align: 'center',
      behavior: isInitial ? 'auto' : 'smooth',
    });
    // Focus the scroll area so PageDown, Space and the arrow keys scroll the strips
    if (isInitial) scrollContainerRef.current?.focus({ preventScroll: true });
    scrolledToDate.current = currentDate;
  }, [currentDate, currentListIndex, virtualizer]);

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
        scrolledToDate.current = listStrips[closestIdx]?.date ?? scrolledToDate.current;
        setCurrentIndex(toList(closestIdx));
      }, 100);
    };

    scrollEl.addEventListener('scroll', handleScroll, { passive: true });
    return () => {
      scrollEl.removeEventListener('scroll', handleScroll);
      if (debounceTimerRef.current) clearTimeout(debounceTimerRef.current);
    };
  }, [strips, listStrips, toList, setCurrentIndex, virtualizer]);

  // Infinite scroll, following TanStack Virtual's infinite-scroll example: load a page
  // when the first or last rendered strip is the edge of the list, one request at a time.
  // The top edge loads older strips, or newer ones with "Newest first". The anchoring above
  // keeps the view still when strips arrive at the top, so the first rendered strip moves
  // away from the edge and this doesn't fire again.
  // Nothing loads until the rendered range reaches the current strip: on open, the first
  // render is laid out from the top before the scroll to the current strip lands, and
  // loading older strips then would anchor the view on the wrong strip.
  const virtualItems = virtualizer.getVirtualItems();
  const firstRenderedIndex = virtualItems[0]?.index;
  const lastRenderedIndex = virtualItems.at(-1)?.index;
  const isFetchingPage = isFetchingOlder || isFetchingNewer;
  useEffect(() => {
    if (scrolledToDate.current === null || isFetchingPage) return;
    if (firstRenderedIndex === undefined || lastRenderedIndex === undefined) return;
    if (currentListIndex < firstRenderedIndex || currentListIndex > lastRenderedIndex) return;

    const [hasAbove, loadAbove, hasBelow, loadBelow] = newestFirst
      ? [hasNewer, loadNewer, hasOlder, loadOlder]
      : [hasOlder, loadOlder, hasNewer, loadNewer];
    if (firstRenderedIndex === 0 && hasAbove) {
      loadAbove();
    } else if (lastRenderedIndex >= strips.length - 1 && hasBelow) {
      loadBelow();
    }
  }, [firstRenderedIndex, lastRenderedIndex, currentListIndex, strips.length, newestFirst, hasOlder, hasNewer, isFetchingPage, loadOlder, loadNewer]);

  // Keyboard navigation. Home/End and J/K move to the top/bottom of the list and down/up it,
  // so they follow the scroll order.
  const goToTop = newestFirst ? handleGoToLast : handleGoToFirst;
  const goToBottom = newestFirst ? handleGoToFirst : handleGoToLast;
  const goDown = newestFirst ? goOlder : goNewer;
  const goUp = newestFirst ? goNewer : goOlder;
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Popovers, menus and the calendar mark the keys they handle (e.g. the
      // Escape that closes them); modifier combos belong to the browser.
      if (e.defaultPrevented || e.altKey || e.ctrlKey || e.metaKey) return;
      // The lightbox handles its own keys while it's open
      if (isLightboxOpen) return;
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return;

      switch (e.key) {
        case 'Home':
          e.preventDefault();
          goToTop();
          break;
        case 'End':
          e.preventDefault();
          goToBottom();
          break;
        case 'r':
        case 'R':
          e.preventDefault();
          goToRandom();
          break;
        case 'j':
        case 'J':
          e.preventDefault();
          goDown();
          break;
        case 'k':
        case 'K':
          e.preventDefault();
          goUp();
          break;
        case 'f':
        case 'F':
          e.preventDefault();
          openFullscreen(currentDate ?? undefined);
          break;
        case 'Escape':
          e.preventDefault();
          goBack();
          break;
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [goToTop, goToBottom, goToRandom, goDown, goUp, openFullscreen, currentDate, goBack, isLightboxOpen]);

  return (
    <div ref={scrollContainerRef} tabIndex={-1} className="h-dvh overflow-y-auto bg-canvas outline-none">
      <ReaderHeader
        comicName={comicName}
        onFirst={handleGoToFirst}
        onLast={handleGoToLast}
        onRandom={goToRandom}
        isLoadingRandom={isLoadingRandom}
        onOlder={goOlder}
        onNewer={goNewer}
        canGoOlder={currentIndex > 0 || hasOlder}
        canGoNewer={currentIndex < strips.length - 1 || hasNewer}
        onFullscreen={viewable.length > 0 ? () => openFullscreen(currentDate ?? undefined) : undefined}
        newestFirst={newestFirst}
        favoriteButton={<FavoriteButton comicId={comicId} comicName={comicName} />}
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
                  key={listStrips[virtualItem.index].date}
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
                    strip={listStrips[virtualItem.index]}
                    comicName={comicName}
                    priority={virtualItem.index === currentListIndex}
                    onOpen={() => openFullscreen(listStrips[virtualItem.index].date)}
                  />
                </div>
              ))}
            </div>
          )}
        </div>
      </main>

      {lightbox.isOpen && (
        <Lightbox
          items={lightboxItems}
          currentIndex={lightbox.currentIndex}
          onClose={closeLightbox}
          onNext={lightbox.next}
          onPrevious={lightbox.previous}
        />
      )}
    </div>
  );
}
