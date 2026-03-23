'use client';

import { forwardRef, useState } from 'react';
import type { Strip } from '@/hooks/use-reader';
import { StripSkeleton } from './strip-skeleton';
import { formatFullDate } from '@/lib/date-utils';

interface StripCardProps {
  strip: Strip;
  comicName: string;
}

export const StripCard = forwardRef<HTMLDivElement, StripCardProps>(
  function StripCard({ strip, comicName }, ref) {
    const [loaded, setLoaded] = useState(false);
    const [error, setError] = useState(false);

    const formattedDate = formatFullDate(strip.date);

    if (!strip.available || !strip.imageUrl) {
      return (
        <div ref={ref} className="py-4" aria-live="polite">
          <p className="text-sm text-ink-muted text-center py-8">
            No strip available for {formattedDate}
          </p>
        </div>
      );
    }

    return (
      <div ref={ref} className="py-4" aria-live="polite">
        <p className="text-sm text-ink-subtle mb-2">{formattedDate}</p>
        <div
          className={`relative overflow-hidden rounded-lg ${strip.width && strip.height ? '' : 'aspect-[3/1]'}`}
          style={strip.width && strip.height ? { aspectRatio: `${strip.width}/${strip.height}` } : undefined}
        >
          {/* Skeleton stays behind image to prevent layout shift */}
          <div className={`absolute inset-0 transition-opacity duration-300 ${loaded ? 'opacity-0' : 'opacity-100'}`}>
            <StripSkeleton />
          </div>
          {error ? (
            <div className="absolute inset-0 bg-card flex items-center justify-center">
              <p className="text-sm text-ink-muted">Failed to load strip</p>
            </div>
          ) : (
            <img
              src={strip.imageUrl}
              alt={`${comicName} - ${formattedDate}`}
              loading="lazy"
              className={`absolute inset-0 w-full h-full object-contain transition-opacity duration-300 ${loaded ? 'opacity-100' : 'opacity-0'}`}
              onLoad={() => setLoaded(true)}
              onError={() => setError(true)}
            />
          )}
        </div>
      </div>
    );
  },
);
