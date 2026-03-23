'use client';

import { cn } from '@/lib/utils';

interface ImageWithFallbackProps {
  src?: string | null;
  alt: string;
  fallbackText: string;
  fit?: 'cover' | 'contain';
  className?: string;
}

export function ImageWithFallback({ src, alt, fallbackText, fit = 'cover', className }: ImageWithFallbackProps) {
  if (src) {
    return (
      <img
        src={src}
        alt={alt}
        className={cn(`w-full h-full object-${fit}`, className)}
      />
    );
  }

  return (
    <div className={cn('w-full h-full flex items-center justify-center text-ink-muted', className)}>
      {fallbackText}
    </div>
  );
}
