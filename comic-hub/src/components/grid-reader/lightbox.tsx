'use client';

import { useCallback } from 'react';
import { Dialog as DialogPrimitive } from 'radix-ui';
import { ChevronLeft, ChevronRight, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { usePinchZoom } from '@/hooks/use-pinch-zoom';
import type { GridComic } from '@/hooks/use-grid-reader';
import { formatFullDate } from '@/lib/date-utils';

interface LightboxProps {
  comics: GridComic[];
  currentIndex: number;
  onClose: () => void;
  onNext: () => void;
  onPrevious: () => void;
}

export function Lightbox({ comics, currentIndex, onClose, onNext, onPrevious }: LightboxProps) {
  const comic = comics[currentIndex];
  const strip = comic?.strip;
  const { state: zoom, handlers: zoomHandlers, isZoomed, resetZoom } = usePinchZoom();

  const handleBackdropClick = useCallback(
    (e: React.MouseEvent) => {
      if (e.target === e.currentTarget) {
        if (isZoomed) {
          resetZoom();
        } else {
          onClose();
        }
      }
    },
    [isZoomed, resetZoom, onClose],
  );

  if (!comic || !strip?.imageUrl) return null;

  const hasPrevious = currentIndex > 0;
  const hasNext = currentIndex < comics.length - 1;

  // Radix Dialog supplies the focus trap, focus return and aria wiring. The
  // content fills the screen, so a click on its empty area is the backdrop click.
  return (
    <DialogPrimitive.Root open onOpenChange={(open) => { if (!open) onClose(); }}>
      <DialogPrimitive.Portal>
        <DialogPrimitive.Overlay className="fixed inset-0 z-modal-backdrop bg-black/80" />
        <DialogPrimitive.Content
          className="fixed inset-0 z-modal flex items-center justify-center outline-none"
          onClick={handleBackdropClick}
          aria-describedby={undefined}
        >
          {/* Strip image (first, so the controls below paint over it) */}
          <div
            className="max-w-[90vw] max-h-[85vh]"
            {...zoomHandlers}
            style={{
              transform: `scale(${zoom.scale}) translate(${zoom.translateX / zoom.scale}px, ${zoom.translateY / zoom.scale}px)`,
              transformOrigin: `${zoom.originX}% ${zoom.originY}%`,
              transition: isZoomed ? 'none' : 'transform 200ms ease-out',
            }}
          >
            <img
              src={strip.imageUrl}
              alt={`${comic.name}, ${formatFullDate(strip.date)}`}
              className="max-w-full max-h-[85vh] object-contain select-none"
              draggable={false}
            />
          </div>

          <DialogPrimitive.Title className="absolute top-4 left-4 font-sans text-white/80 text-sm font-medium">
            {comic.name}
          </DialogPrimitive.Title>

          <Button
            variant="ghost"
            size="icon"
            onClick={onClose}
            className="absolute top-4 right-4 text-white/70 hover:text-white hover:bg-white/10"
            aria-label="Close lightbox"
          >
            <X className="h-6 w-6" />
          </Button>

          {hasPrevious && (
            <Button
              variant="ghost"
              size="icon"
              onClick={onPrevious}
              className="absolute left-4 top-1/2 -translate-y-1/2 h-16 w-12 text-white/70 hover:text-white hover:bg-white/10"
              aria-label="Previous comic"
            >
              <ChevronLeft className="h-8 w-8" />
            </Button>
          )}

          {hasNext && (
            <Button
              variant="ghost"
              size="icon"
              onClick={onNext}
              className="absolute right-4 top-1/2 -translate-y-1/2 h-16 w-12 text-white/70 hover:text-white hover:bg-white/10"
              aria-label="Next comic"
            >
              <ChevronRight className="h-8 w-8" />
            </Button>
          )}
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  );
}
