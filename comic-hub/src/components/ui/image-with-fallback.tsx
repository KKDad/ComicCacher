'use client';

import { useState } from 'react';
import Image from 'next/image';
import { cn } from '@/lib/utils';

interface ImageWithFallbackProps {
  src?: string | null;
  alt: string;
  fallbackText: string;
  /** Rendered width for the optimizer, e.g. `144px` or `(max-width: 640px) 100vw, 25vw` */
  sizes: string;
  fit?: 'cover' | 'contain';
  className?: string;
}

/** Fills its box with the image, or with `fallbackText` when there is no image or it fails to load. */
export function ImageWithFallback({ src, alt, fallbackText, sizes, fit = 'cover', className }: ImageWithFallbackProps) {
  const [failedSrc, setFailedSrc] = useState<string | null>(null);

  if (src && src !== failedSrc) {
    return (
      <div className="relative w-full h-full">
        <Image
          src={src}
          alt={alt}
          fill
          sizes={sizes}
          className={cn(`object-${fit}`, className)}
          onError={() => setFailedSrc(src)}
        />
      </div>
    );
  }

  return (
    <div className={cn('w-full h-full flex items-center justify-center text-ink-muted', className)}>
      {fallbackText}
    </div>
  );
}
