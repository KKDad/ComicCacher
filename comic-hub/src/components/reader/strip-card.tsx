'use client';

import { forwardRef, useState } from 'react';
import Image from 'next/image';
import type { Strip } from '@/hooks/use-reader';
import { StripSkeleton } from './strip-skeleton';
import { formatFullDate } from '@/lib/date-utils';
import { BrokenIllustration } from '@/components/illustrations';
import { STRIP_SIZES } from './strip-sizes';

interface StripCardProps {
  strip: Strip;
  comicName: string;
  /** Load this strip first: the one the reader opens on is the page's largest image. */
  priority?: boolean;
  /** Opens the strip fullscreen. */
  onOpen?: () => void;
}

export const StripCard = forwardRef<HTMLDivElement, StripCardProps>(
  function StripCard({ strip, comicName, priority = false, onOpen }, ref) {
    const [loaded, setLoaded] = useState(false);
    const [error, setError] = useState(false);

    const formattedDate = formatFullDate(strip.date);

    if (!strip.available || !strip.imageUrl) {
      return (
        <div ref={ref} className="py-4">
          <p className="text-sm text-ink-subtle text-center py-8">
            No strip available for {formattedDate}
          </p>
        </div>
      );
    }

    return (
      <div ref={ref} className="py-4">
        <h2 className="text-sm font-semibold text-ink-subtle mb-2">{formattedDate}</h2>
        {/* The strip sits on a paper mat so white strips don't merge into the page */}
        <div className="rounded-xl border border-border bg-strip-mat p-2 shadow-xs">
        <div
          className={`relative overflow-hidden rounded-md ${strip.width && strip.height ? '' : 'aspect-[3/1]'}`}
          style={strip.width && strip.height ? { aspectRatio: `${strip.width}/${strip.height}` } : undefined}
        >
          {/* Skeleton stays behind image to prevent layout shift */}
          <div className={`absolute inset-0 transition-opacity duration-300 ${loaded ? 'opacity-0' : 'opacity-100'}`}>
            <StripSkeleton />
          </div>
          {error ? (
            <div className="absolute inset-0 bg-card flex flex-col items-center justify-center gap-1 p-2">
                <BrokenIllustration className="h-3/5 max-h-28 w-auto" />
                <p className="text-sm text-ink-subtle">This strip didn&rsquo;t load</p>
              </div>
          ) : (
            <Image
              src={strip.imageUrl}
              alt={`${comicName} - ${formattedDate}`}
              fill
              sizes={STRIP_SIZES}
              loading={priority ? 'eager' : 'lazy'}
              fetchPriority={priority ? 'high' : 'auto'}
              className={`strip-image object-contain transition-opacity duration-300 ${loaded ? 'opacity-100' : 'opacity-0'}`}
              onLoad={() => setLoaded(true)}
              onError={() => setError(true)}
            />
          )}
          {onOpen && !error && (
            <button
              type="button"
              onClick={onOpen}
              aria-label={`View ${comicName}, ${formattedDate} fullscreen`}
              className="absolute inset-0 cursor-zoom-in focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary rounded-md"
            />
          )}
        </div>
        </div>
      </div>
    );
  },
);
