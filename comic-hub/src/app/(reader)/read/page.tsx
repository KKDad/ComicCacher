'use client';

import { useSearchParams } from 'next/navigation';
import { GridReader } from '@/components/grid-reader/grid-reader';

export default function GridReaderPage() {
  const searchParams = useSearchParams();
  const dateParam = searchParams.get('date') ?? undefined;

  return <GridReader initialDate={dateParam} />;
}
