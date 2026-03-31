'use client';

import { useResponsiveNav } from '@/hooks/use-responsive-nav';
import { useGridReader } from '@/hooks/use-grid-reader';
import { DesktopGridReader } from './desktop-grid-reader';
import { MobileGridReader } from './mobile-grid-reader';

interface GridReaderProps {
  initialDate?: string;
}

export function GridReader({ initialDate }: GridReaderProps) {
  const { layout } = useResponsiveNav();
  const isMobile = layout === 'mobile';

  const reader = useGridReader({ initialDate });

  if (isMobile) {
    return <MobileGridReader reader={reader} />;
  }

  return <DesktopGridReader reader={reader} />;
}
