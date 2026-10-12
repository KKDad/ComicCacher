'use client';

import { useResponsiveNav } from '@/hooks/use-responsive-nav';
import { useReader } from '@/hooks/use-reader';
import { usePhoneLandscape } from '@/hooks/use-phone-landscape';
import { DesktopReader } from './desktop-reader';
import { MobileReader } from './mobile-reader';
import { StripSkeleton } from './strip-skeleton';

interface ComicReaderProps {
  comicId: number;
  initialDate?: string;
}

export function ComicReader({ comicId, initialDate }: ComicReaderProps) {
  const { layout } = useResponsiveNav();
  // A phone turned sideways is wider than md, but keeps the swipe reader
  const phoneLandscape = usePhoneLandscape();
  const isMobile = layout === 'mobile' || phoneLandscape;

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

  if (isMobile) {
    return <MobileReader comicId={comicId} reader={reader} />;
  }

  return <DesktopReader comicId={comicId} reader={reader} />;
}
