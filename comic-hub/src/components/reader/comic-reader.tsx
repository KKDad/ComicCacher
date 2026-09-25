'use client';

import { useResponsiveNav } from '@/hooks/use-responsive-nav';
import { useReader } from '@/hooks/use-reader';
import { DesktopReader } from './desktop-reader';
import { MobileReader } from './mobile-reader';
import { StripSkeleton } from './strip-skeleton';

interface ComicReaderProps {
  comicId: number;
  initialDate?: string;
}

export function ComicReader({ comicId, initialDate }: ComicReaderProps) {
  const { layout } = useResponsiveNav();
  const isMobile = layout === 'mobile';

  const reader = useReader({
    comicId,
    initialDate,
    mode: isMobile ? 'snap' : 'scroll',
  });

  // Until the browser reports its width, show a skeleton rather than guess a
  // layout and swap it out after hydration.
  if (layout === null) {
    return (
      <div className="min-h-dvh bg-canvas flex items-center justify-center p-4" aria-busy="true">
        <StripSkeleton className="w-full max-w-3xl" />
      </div>
    );
  }

  return isMobile ? <MobileReader comicId={comicId} reader={reader} /> : <DesktopReader reader={reader} />;
}
