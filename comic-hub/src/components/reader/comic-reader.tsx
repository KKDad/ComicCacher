'use client';

import { useResponsiveNav } from '@/hooks/use-responsive-nav';
import { useReader } from '@/hooks/use-reader';
import { DesktopReader } from './desktop-reader';
import { MobileReader } from './mobile-reader';

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

  if (isMobile) {
    return <MobileReader comicId={comicId} reader={reader} />;
  }

  return <DesktopReader comicId={comicId} reader={reader} />;
}
