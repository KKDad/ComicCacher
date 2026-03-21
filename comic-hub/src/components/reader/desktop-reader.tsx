'use client';

import { useCallback, useEffect, useRef } from 'react';
import type { useReader } from '@/hooks/use-reader';
import { ReaderHeader } from './reader-header';
import { StripCard } from './strip-card';
import { StripSkeleton } from './strip-skeleton';
import { CrossComicNav } from './cross-comic-nav';
import { DatePickerPopover } from './date-picker-popover';
import { useReadingList } from '@/hooks/use-reading-list';

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
    loadOlder,
    loadNewer,
    goToDate,
    goToFirst,
    goToLast,
    goToRandom,
    isLoadingRandom,
  } = reader;

  const { previousComic, nextComic, navigateToComic } = useReadingList(comicId);

  const stripRefs = useRef<Map<number, HTMLDivElement>>(new Map());
  const containerRef = useRef<HTMLDivElement>(null);
  const olderSentinelRef = useRef<HTMLDivElement>(null);
  const newerSentinelRef = useRef<HTMLDivElement>(null);
  const initialScrollDone = useRef(false);

  // Scroll to starting strip on initial load
  useEffect(() => {
    if (!initialScrollDone.current && strips.length > 0 && currentIndex >= 0) {
      const el = stripRefs.current.get(currentIndex);
      if (el) {
        el.scrollIntoView({ block: 'center' });
        initialScrollDone.current = true;
      }
    }
  }, [strips, currentIndex]);

  const debounceTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // IntersectionObserver for tracking current strip — only after initial scroll
  useEffect(() => {
    if (strips.length === 0 || !initialScrollDone.current) return;

    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            const idx = Number(entry.target.getAttribute('data-index'));
            if (!isNaN(idx)) {
              if (debounceTimerRef.current) clearTimeout(debounceTimerRef.current);
              debounceTimerRef.current = setTimeout(() => setCurrentIndex(idx), 100);
            }
          }
        }
      },
      { threshold: 0.5 },
    );

    for (const [, el] of stripRefs.current) {
      observer.observe(el);
    }

    return () => {
      observer.disconnect();
      if (debounceTimerRef.current) clearTimeout(debounceTimerRef.current);
    };
  }, [strips, setCurrentIndex]);

  // Sentinel observers for infinite scroll — only after initial scroll
  useEffect(() => {
    if (!initialScrollDone.current) return;

    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            if (entry.target === olderSentinelRef.current && hasOlder) {
              loadOlder();
            } else if (entry.target === newerSentinelRef.current && hasNewer) {
              loadNewer();
            }
          }
        }
      },
      { rootMargin: '200px' },
    );

    if (olderSentinelRef.current) observer.observe(olderSentinelRef.current);
    if (newerSentinelRef.current) observer.observe(newerSentinelRef.current);

    return () => observer.disconnect();
  }, [hasOlder, hasNewer, loadOlder, loadNewer]);

  // Keyboard navigation
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Don't handle if user is typing in an input
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return;

      switch (e.key) {
        case 'Home':
          e.preventDefault();
          goToFirst();
          break;
        case 'End':
          e.preventDefault();
          goToLast();
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
        case 'ArrowLeft':
          if (previousComic) {
            e.preventDefault();
            navigateToComic(previousComic.id);
          }
          break;
        case 'ArrowRight':
          if (nextComic) {
            e.preventDefault();
            navigateToComic(nextComic.id);
          }
          break;
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [goToFirst, goToLast, goToRandom, previousComic, nextComic, navigateToComic]);

  const setStripRef = useCallback(
    (index: number) => (el: HTMLDivElement | null) => {
      if (el) {
        stripRefs.current.set(index, el);
      } else {
        stripRefs.current.delete(index);
      }
    },
    [],
  );

  return (
    <div ref={containerRef} className="min-h-screen bg-canvas">
      <ReaderHeader
        comicName={comicName}
        onFirst={goToFirst}
        onLast={goToLast}
        onRandom={goToRandom}
        isLoadingRandom={isLoadingRandom}
        hasOlder={hasOlder}
        hasNewer={hasNewer}
        datePicker={
          <DatePickerPopover
            oldest={oldest}
            newest={newest}
            currentDate={strips[currentIndex]?.date ?? null}
            onSelectDate={goToDate}
          />
        }
      />

      <CrossComicNav comicId={comicId} />

      <main className="pt-14 pb-8 px-4">
        <div className="max-w-3xl mx-auto">
          {/* Older sentinel */}
          <div ref={olderSentinelRef} className="h-1" />

          {isLoading ? (
            <div className="space-y-6 py-4">
              <StripSkeleton className="bg-card rounded-lg p-4" />
              <StripSkeleton className="bg-card rounded-lg p-4" />
              <StripSkeleton className="bg-card rounded-lg p-4" />
            </div>
          ) : (
            strips.map((strip, index) => (
              <div key={strip.date} data-index={index} ref={setStripRef(index)}>
                <StripCard strip={strip} comicName={comicName} />
              </div>
            ))
          )}

          {/* Newer sentinel */}
          <div ref={newerSentinelRef} className="h-1" />
        </div>
      </main>
    </div>
  );
}
