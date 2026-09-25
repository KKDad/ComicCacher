'use client';

import { Card, CardContent, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { BookOpen } from 'lucide-react';
import { Skeleton } from '@/components/ui/skeleton';
import Link from 'next/link';
import { ImageWithFallback } from '@/components/ui/image-with-fallback';
import { EmptyState } from '@/components/ui/empty-state';
import { formatMediumDate } from '@/lib/date-utils';

interface LastRead {
  comic: {
    id: number;
    name: string;
    lastStrip?: { imageUrl?: string | null } | null;
  };
  date: string;
}

interface ContinueReadingProps {
  lastRead?: LastRead | null;
  isLoading?: boolean;
}

export function ContinueReading({ lastRead = null, isLoading = false }: ContinueReadingProps) {
  const readUpTo = lastRead ? formatMediumDate(lastRead.date) : '';

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
        <div className="max-w-sm">
          <EmptyState
            icon={BookOpen}
            title="No recent reading history"
            description="Start reading to see your progress here"
            actionLabel="Browse Comics"
            actionHref="/comics"
          />
        </div>
      </section>
    );
  }

  return (
    <section>
      <h2 className="text-xl font-semibold mb-4 text-ink">
        Continue Where You Left Off
      </h2>
      <Card className="max-w-sm hover:shadow-md transition-shadow">
        <CardContent className="p-6">
          <div className="aspect-[3/4] bg-canvas rounded-lg mb-4 overflow-hidden">
            <ImageWithFallback
              src={lastRead.comic.lastStrip?.imageUrl}
              alt=""
              fallbackText={lastRead.comic.name[0]}
            />
          </div>
          <h3 className="font-sans text-lg font-semibold mb-1">{lastRead.comic.name}</h3>
          <CardDescription>Read up to {readUpTo}</CardDescription>
          <Button asChild className="w-full mt-4">
            <Link
              href={`/comics/${lastRead.comic.id}/read?date=${lastRead.date}`}
              aria-label={`Continue reading ${lastRead.comic.name}`}
            >
              Continue reading
            </Link>
          </Button>
        </CardContent>
      </Card>
    </section>
  );
}
