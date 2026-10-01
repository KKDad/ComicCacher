'use client';

import { Loader2 } from 'lucide-react';
import { ImageWithFallback } from '@/components/ui/image-with-fallback';
import { cn } from '@/lib/utils';

/** Up to two initials from a comic's name, for when it has no picture yet. */
export function initials(name: string): string {
  const words = name.replace(/[^\p{L}\p{N}\s]/gu, ' ').split(/\s+/).filter(Boolean);
  if (words.length === 0) return '?';
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase();
  return (words[0][0] + words[1][0]).toUpperCase();
}

interface ComicThumbProps {
  name: string;
  src?: string | null;
  pending?: boolean;
  className?: string;
}

/** A small square picture of a comic: its avatar or catalog thumbnail, or its initials. */
export function ComicThumb({ name, src, pending = false, className }: ComicThumbProps) {
  return (
    <div className={cn('relative size-12 shrink-0 overflow-hidden rounded-md border bg-muted text-sm font-semibold', className)}>
      <ImageWithFallback src={src} alt="" fallbackText={initials(name)} sizes="48px" fit="contain" />
      {pending && (
        <span className="absolute bottom-0.5 right-0.5 rounded-full bg-background/80 p-0.5" title="Downloading picture">
          <Loader2 className="size-3 animate-spin text-muted-foreground" aria-hidden />
          <span className="sr-only">Downloading picture</span>
        </span>
      )}
    </div>
  );
}
