'use client';

import { useParams, useSearchParams } from 'next/navigation';
import { useGetUserPreferencesQuery } from '@/generated/graphql';
import { ComicReader } from '@/components/reader/comic-reader';
import { Skeleton } from '@/components/ui/skeleton';

export default function ReaderPage() {
  const params = useParams();
  const searchParams = useSearchParams();
  const comicId = parseInt(params.id as string);
  const dateParam = searchParams.get('date') ?? undefined;

  const { data: prefsData, isLoading: prefsLoading } = useGetUserPreferencesQuery(
    undefined,
    { staleTime: 5 * 60 * 1000 },
  );

  // Resolve start date: URL param > last read > undefined (will use newest)
  const lastReadDate = prefsData?.preferences?.lastReadDates?.find(
    (lr) => lr.comicId === comicId,
  )?.date;
  const startDate = dateParam ?? lastReadDate ?? undefined;

  if (prefsLoading && !dateParam) {
    return (
      <div className="min-h-screen bg-canvas flex items-center justify-center">
        <div className="w-full max-w-3xl mx-auto p-4 space-y-4">
          <Skeleton className="h-8 w-48 bg-muted" />
          <Skeleton className="w-full aspect-[3/1] bg-muted" />
        </div>
      </div>
    );
  }

  return <ComicReader comicId={comicId} initialDate={startDate} />;
}
