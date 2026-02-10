'use client';

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { BookOpen } from 'lucide-react';
import { Skeleton } from '@/components/ui/skeleton';
import { useGetUserPreferencesQuery, useGetComicQuery, GetComicQuery } from '@/generated/graphql';
import { useMemo } from 'react';
import Link from 'next/link';
import { UseQueryOptions } from '@tanstack/react-query';

export function ContinueReading() {
  // Fetch user preferences (contains lastReadDates)
  const { data: preferencesData, isLoading: preferencesLoading } = useGetUserPreferencesQuery();

  // Find the most recent read entry
  const lastReadEntry = useMemo(() => {
    const lastReadDates = preferencesData?.preferences?.lastReadDates || [];
    if (lastReadDates.length === 0) return null;

    // Sort by date descending and take the most recent
    return [...lastReadDates].sort((a, b) =>
      new Date(b.date).getTime() - new Date(a.date).getTime()
    )[0];
  }, [preferencesData]);

  // Fetch the comic details for the last read comic (only if we have a comic ID)
  const comicId = lastReadEntry?.comicId || 0;
  const queryOptions: Partial<UseQueryOptions<GetComicQuery>> = {
    enabled: comicId > 0,
    retry: false,
  };
  const { data: comicData, isLoading: comicLoading } = useGetComicQuery(
    { id: comicId },
    queryOptions as any
  );

  const isLoading = preferencesLoading || (comicId > 0 && comicLoading);
  const lastRead = lastReadEntry && comicData?.comic ? {
    comic: comicData.comic,
    date: lastReadEntry.date,
  } : null;

  if (isLoading) {
    return (
      <section>
        <h2 className="text-xl font-semibold mb-4 text-ink">
          Continue Where You Left Off
        </h2>
        <Card className="max-w-sm">
          <CardContent className="p-6">
            <Skeleton className="h-48 w-full mb-4 rounded-lg" />
            <Skeleton className="h-6 w-3/4 mb-2" />
            <Skeleton className="h-4 w-1/2" />
          </CardContent>
        </Card>
      </section>
    );
  }

  if (!lastRead) {
    return (
      <section>
        <h2 className="text-xl font-semibold mb-4 text-ink">
          Continue Where You Left Off
        </h2>
        <Card className="max-w-sm border-dashed">
          <CardContent className="flex flex-col items-center justify-center p-12 text-center">
            <BookOpen className="h-12 w-12 text-ink-muted mb-4" />
            <p className="text-ink-subtle mb-2">No recent reading history</p>
            <p className="text-sm text-ink-muted mb-4">
              Start reading to see your progress here
            </p>
            <Button>Browse Comics</Button>
          </CardContent>
        </Card>
      </section>
    );
  }

  // Calculate how long ago the comic was read
  const timeAgo = useMemo(() => {
    if (!lastRead) return '';
    const lastReadDate = new Date(lastRead.date);
    const now = new Date();
    const diffMs = now.getTime() - lastReadDate.getTime();
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (diffDays === 0) return 'today';
    if (diffDays === 1) return '1 day ago';
    return `${diffDays} days ago`;
  }, [lastRead]);

  return (
    <section>
      <h2 className="text-xl font-semibold mb-4 text-ink">
        Continue Where You Left Off
      </h2>
      <Card className="max-w-sm hover:shadow-md transition-shadow">
        <CardContent className="p-6">
          <div className="aspect-[3/4] bg-canvas rounded-lg mb-4 overflow-hidden">
            {lastRead?.comic.lastStrip?.imageUrl ? (
              <img
                src={lastRead.comic.lastStrip.imageUrl}
                alt={lastRead.comic.name}
                className="w-full h-full object-cover"
              />
            ) : (
              <div className="w-full h-full flex items-center justify-center text-ink-muted">
                {lastRead?.comic.name[0]}
              </div>
            )}
          </div>
          <CardTitle className="text-lg mb-1">{lastRead?.comic.name}</CardTitle>
          <CardDescription>Last read: {timeAgo}</CardDescription>
          <Link href={`/comics/${lastRead?.comic.id}/${lastRead?.date}`}>
            <Button className="w-full mt-4">Continue Reading</Button>
          </Link>
        </CardContent>
      </Card>
    </section>
  );
}
