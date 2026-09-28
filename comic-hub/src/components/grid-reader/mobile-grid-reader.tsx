'use client';

import { GridHeader } from './grid-header';
import { GridStripCard } from './grid-strip-card';
import { Lightbox } from './lightbox';
import { TodayEmptyState } from './today-empty-state';
import { StripSkeleton } from '@/components/reader/strip-skeleton';
import { useLightbox } from '@/hooks/use-lightbox';
import { useRandomStrip } from '@/hooks/use-random-strip';
import { useSwipe } from '@/hooks/use-swipe';
import { toLightboxItems, type useGridReader } from '@/hooks/use-grid-reader';

interface MobileGridReaderProps {
  reader: ReturnType<typeof useGridReader>;
}

export function MobileGridReader({ reader }: MobileGridReaderProps) {
  const { date, comics, isLoading, goToDate, goToNextDate, goToPreviousDate, goToToday } = reader;
  const lightbox = useLightbox(comics.length);

  const { goToRandom: handleRandom } = useRandomStrip(goToDate);

  // Horizontal swipe for date navigation
  const swipeHandlers = useSwipe({
    onSwipeLeft: goToNextDate,
    onSwipeRight: goToPreviousDate,
  });

  return (
    <div className="h-dvh overflow-y-auto bg-canvas" {...swipeHandlers}>
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
          <TodayEmptyState />
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
          items={toLightboxItems(comics)}
          currentIndex={lightbox.currentIndex}
          onClose={lightbox.close}
          onNext={lightbox.next}
          onPrevious={lightbox.previous}
        />
      )}
    </div>
  );
}
