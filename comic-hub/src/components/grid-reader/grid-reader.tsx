'use client';

import { useResponsiveNav } from '@/hooks/use-responsive-nav';
import { useGridReader } from '@/hooks/use-grid-reader';
import { DesktopGridReader } from './desktop-grid-reader';
import { MobileGridReader } from './mobile-grid-reader';
import { StripSkeleton } from '@/components/reader/strip-skeleton';

interface GridReaderProps {
  initialDate?: string;
}

export function GridReader({ initialDate }: GridReaderProps) {
  const { layout } = useResponsiveNav();
  const isMobile = layout === 'mobile';

  const reader = useGridReader({ initialDate });

  if (layout === null) {
    return (
      <div className="min-h-dvh bg-canvas px-4 pt-18 space-y-6" aria-busy="true">
        <StripSkeleton className="max-w-3xl mx-auto bg-card rounded-lg p-4" />
        <StripSkeleton className="max-w-3xl mx-auto bg-card rounded-lg p-4" />
      </div>
    );
  }

  return isMobile ? <MobileGridReader reader={reader} /> : <DesktopGridReader reader={reader} />;
}
