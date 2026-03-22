'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { useRouter } from 'next/navigation';
import type { useReader } from '@/hooks/use-reader';
import { useSwipe } from '@/hooks/use-swipe';
import { usePinchZoom } from '@/hooks/use-pinch-zoom';
import { Button } from '@/components/ui/button';
import { ArrowLeft, ChevronUp, ChevronDown } from 'lucide-react';
import { ReaderControls } from './reader-controls';
import { StripSkeleton } from './strip-skeleton';
import { ReadingListDrawer } from './reading-list-drawer';
import { DatePickerPopover } from './date-picker-popover';

/** Duration (ms) for the swipe transition animation. */
const SWIPE_TRANSITION_MS = 200;
/** Duration (ms) for the rubber-band bounce snap-back. */
const BOUNCE_SNAP_MS = 100;

interface MobileReaderProps {
  comicId: number;
  reader: ReturnType<typeof useReader>;
}

export function MobileReader({ comicId, reader }: MobileReaderProps) {
  const router = useRouter();
  const {
    strips,
    currentIndex,
    comicName,
    oldest,
    newest,
    hasOlder,
    hasNewer,
    isLoading,
    goToDate,
    goToFirst,
    goToLast,
    goToRandom,
    goNewer,
    goOlder,
    isLoadingRandom,
  } = reader;

  const [controlsVisible, setControlsVisible] = useState(false);
  const [translateY, setTranslateY] = useState(0);
  const [isAnimating, setIsAnimating] = useState(false);
  const hideTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const { state: zoomState, handlers: zoomHandlers, isZoomed, resetZoom } = usePinchZoom();

  const currentStrip = strips[currentIndex] ?? null;
  const prevStrip = strips[currentIndex - 1] ?? null;
  const nextStrip = strips[currentIndex + 1] ?? null;

  const formattedDate = currentStrip
    ? new Date(currentStrip.date).toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
      })
    : '';

  // Auto-hide controls after navigation
  const hideControlsAfterNav = useCallback(() => {
    if (hideTimerRef.current) clearTimeout(hideTimerRef.current);
    setControlsVisible(false);
  }, []);

  const handleSwipeUp = useCallback(() => {
    if (currentIndex >= strips.length - 1 && !hasNewer) {
      // Rubber-band bounce
      setTranslateY(-30);
      setIsAnimating(true);
      setTimeout(() => {
        setTranslateY(0);
        setTimeout(() => setIsAnimating(false), SWIPE_TRANSITION_MS);
      }, BOUNCE_SNAP_MS);
      return;
    }
    // Animate out then navigate
    setTranslateY(-100);
    setIsAnimating(true);
    setTimeout(() => {
      goNewer();
      setTranslateY(0);
      setIsAnimating(false);
      hideControlsAfterNav();
    }, SWIPE_TRANSITION_MS);
  }, [currentIndex, strips.length, hasNewer, goNewer, hideControlsAfterNav]);

  const handleSwipeDown = useCallback(() => {
    if (currentIndex === 0 && !hasOlder) {
      // Rubber-band bounce
      setTranslateY(30);
      setIsAnimating(true);
      setTimeout(() => {
        setTranslateY(0);
        setTimeout(() => setIsAnimating(false), SWIPE_TRANSITION_MS);
      }, BOUNCE_SNAP_MS);
      return;
    }
    setTranslateY(100);
    setIsAnimating(true);
    setTimeout(() => {
      goOlder();
      setTranslateY(0);
      setIsAnimating(false);
      hideControlsAfterNav();
    }, SWIPE_TRANSITION_MS);
  }, [currentIndex, hasOlder, goOlder, hideControlsAfterNav]);

  const swipeHandlers = useSwipe({
    onSwipeUp: isZoomed ? undefined : handleSwipeUp,
    onSwipeDown: isZoomed ? undefined : handleSwipeDown,
    threshold: 50,
  });

  // Merge swipe + zoom touch handlers
  const mergedTouchHandlers = {
    onTouchStart: (e: React.TouchEvent) => {
      if (e.touches.length >= 2) {
        zoomHandlers.onTouchStart(e);
      } else {
        swipeHandlers.onTouchStart(e);
        zoomHandlers.onTouchStart(e);
      }
    },
    onTouchMove: (e: React.TouchEvent) => {
      if (e.touches.length >= 2 || isZoomed) {
        zoomHandlers.onTouchMove(e);
      } else {
        swipeHandlers.onTouchMove(e);
      }
    },
    onTouchEnd: (e: React.TouchEvent) => {
      swipeHandlers.onTouchEnd(e);
      zoomHandlers.onTouchEnd(e);
    },
  };

  const toggleControls = useCallback(() => {
    if (isZoomed) return;
    setControlsVisible((v) => !v);
  }, [isZoomed]);

  // Reset zoom when strip changes
  useEffect(() => {
    resetZoom();
  }, [currentIndex, resetZoom]);

  // Dismiss controls on Escape
  useEffect(() => {
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && controlsVisible) {
        setControlsVisible(false);
      }
    };
    window.addEventListener('keydown', handleKey);
    return () => window.removeEventListener('keydown', handleKey);
  }, [controlsVisible]);

  return (
    <div
      className="fixed inset-0 bg-canvas flex flex-col overflow-hidden"
      style={{
        paddingTop: 'env(safe-area-inset-top)',
        paddingBottom: 'env(safe-area-inset-bottom)',
      }}
    >
      {/* Content area with swipe */}
      <div
        className="flex-1 flex items-center justify-center px-2 overflow-hidden relative"
        onClick={toggleControls}
        {...mergedTouchHandlers}
      >
        {isLoading ? (
          <StripSkeleton className="w-full" />
        ) : (
          <div
            className="w-full flex flex-col items-center"
            style={{
              transform: `translateY(${translateY}px)`,
              transition: isAnimating ? 'transform 200ms ease-out' : 'none',
            }}
          >
            {currentStrip?.imageUrl && currentStrip.available ? (
              <>
                <img
                  src={currentStrip.imageUrl}
                  alt={`${comicName} - ${formattedDate}`}
                  className="w-full h-auto max-h-[75vh] object-contain"
                  style={{
                    transform: `scale(${zoomState.scale}) translate(${zoomState.translateX / zoomState.scale}px, ${zoomState.translateY / zoomState.scale}px)`,
                    transformOrigin: `${zoomState.originX}% ${zoomState.originY}%`,
                    transition: isZoomed ? 'none' : 'transform 200ms ease-out',
                  }}
                />
                <p className="text-sm text-ink-subtle mt-3">{formattedDate}</p>
              </>
            ) : (
              <p className="text-sm text-ink-muted">
                {currentStrip ? 'No strip available for this date' : 'No strips loaded'}
              </p>
            )}
          </div>
        )}
      </div>

      {/* Controls overlay */}
      <div
        className={`absolute inset-0 pointer-events-none transition-opacity duration-200 ${controlsVisible ? 'opacity-100' : 'opacity-0'}`}
      >
        {/* Top bar */}
        <div
          className="absolute top-0 left-0 right-0 z-modal bg-canvas/80 backdrop-blur-sm flex items-center px-3 h-12 pointer-events-auto"
          style={{ paddingTop: 'env(safe-area-inset-top)' }}
        >
          <Button
            variant="ghost"
            size="icon"
            onClick={() => router.back()}
            aria-label="Go back"
            className="text-ink-subtle hover:text-ink hover:bg-muted"
            tabIndex={controlsVisible ? 0 : -1}
          >
            <ArrowLeft className="h-5 w-5" />
          </Button>
          <span className="text-sm font-medium text-ink truncate ml-2 flex-1">
            {comicName}
          </span>
          <ReadingListDrawer comicId={comicId} />
        </div>

        {/* Bottom bar */}
        <div
          className="absolute bottom-0 left-0 right-0 z-modal bg-canvas/80 backdrop-blur-sm px-3 py-2 pointer-events-auto"
          style={{ paddingBottom: 'env(safe-area-inset-bottom)' }}
        >
          <div className="flex items-center justify-between">
            <Button
              variant="ghost"
              size="icon"
              onClick={goOlder}
              disabled={currentIndex === 0 && !hasOlder}
              aria-label="Older strip"
              className="text-ink-subtle hover:text-ink hover:bg-muted"
              tabIndex={controlsVisible ? 0 : -1}
            >
              <ChevronDown className="h-5 w-5" />
            </Button>

            <ReaderControls
              onFirst={goToFirst}
              onLast={goToLast}
              onRandom={goToRandom}
              isLoadingRandom={isLoadingRandom}
              datePicker={
                <DatePickerPopover
                  oldest={oldest}
                  newest={newest}
                  currentDate={currentStrip?.date ?? null}
                  onSelectDate={goToDate}
                />
              }
            />

            <Button
              variant="ghost"
              size="icon"
              onClick={goNewer}
              disabled={currentIndex === strips.length - 1 && !hasNewer}
              aria-label="Newer strip"
              className="text-ink-subtle hover:text-ink hover:bg-muted"
              tabIndex={controlsVisible ? 0 : -1}
            >
              <ChevronUp className="h-5 w-5" />
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
