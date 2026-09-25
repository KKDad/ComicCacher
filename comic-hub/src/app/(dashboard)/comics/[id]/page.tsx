'use client';

import { useParams } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { ArrowLeft, Calendar, User } from 'lucide-react';
import Link from 'next/link';
import { useGetComicQuery } from '@/generated/graphql';
import { formatFullDate, formatMediumDate, parseDate } from '@/lib/date-utils';

export default function ComicDetailPage() {
  const params = useParams();
  const comicId = parseInt(params.id as string);

  const { data, isLoading } = useGetComicQuery({ id: comicId });
  const comic = data?.comic ?? null;

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-10 w-48" />
        <div className="grid md:grid-cols-2 gap-6">
          <Card>
            <CardContent className="p-6">
              <Skeleton className="aspect-square w-full mb-4" />
              <Skeleton className="h-6 w-3/4 mb-2" />
              <Skeleton className="h-4 w-1/2" />
            </CardContent>
          </Card>
          <Card>
            <CardHeader>
              <Skeleton className="h-6 w-32 mb-2" />
              <Skeleton className="h-4 w-full" />
            </CardHeader>
            <CardContent>
              <Skeleton className="h-20 w-full" />
            </CardContent>
          </Card>
        </div>
      </div>
    );
  }

  if (!comic) {
    return (
      <div className="space-y-6">
        <Card>
          <CardContent className="p-12 text-center">
            <p className="text-ink-subtle mb-4">Failed to load comic details</p>
            <Button asChild>
              <Link href="/comics">Browse comics</Link>
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button asChild variant="ghost" size="icon">
          <Link href="/comics" aria-label="Back to comics list">
            <ArrowLeft className="h-5 w-5" />
          </Link>
        </Button>
        <h1 className="text-2xl font-bold text-ink">{comic.name}</h1>
      </div>

      <div className="grid md:grid-cols-2 gap-6">
        <Card>
          <CardContent className="p-6">
            <div className="aspect-square bg-canvas rounded-lg mb-4 overflow-hidden flex items-center justify-center">
              {comic.avatarUrl ? (
                <img
                  src={comic.avatarUrl}
                  alt={comic.name}
                  className="w-full h-full object-cover"
                />
              ) : (
                <div className="text-6xl font-bold text-ink-muted">
                  {comic.name[0]}
                </div>
              )}
            </div>
            <div className="space-y-2">
              {comic.author && (
                <div className="flex items-center gap-2 text-sm text-ink-subtle">
                  <User className="h-4 w-4" aria-hidden="true" />
                  <span>{comic.author}</span>
                </div>
              )}
              {comic.oldest && comic.newest && (
                <div className="flex items-center gap-2 text-sm text-ink-subtle">
                  <Calendar className="h-4 w-4" aria-hidden="true" />
                  <span>
                    {parseDate(comic.oldest).getFullYear()}–{parseDate(comic.newest).getFullYear()}
                  </span>
                </div>
              )}
            </div>
          </CardContent>
        </Card>

        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>About</CardTitle>
              <CardDescription>
                {comic.description || 'No description available'}
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-2 text-sm">
                {comic.source && (
                  <div>
                    <span className="font-medium">Source:</span> {comic.source}
                  </div>
                )}
                {comic.oldest && (
                  <div>
                    <span className="font-medium">First strip:</span>{' '}
                    {formatMediumDate(comic.oldest)}
                  </div>
                )}
                {comic.newest && (
                  <div>
                    <span className="font-medium">Latest strip:</span>{' '}
                    {formatMediumDate(comic.newest)}
                  </div>
                )}
              </div>
            </CardContent>
          </Card>

          {comic.lastStrip && (
            <Card>
              <CardHeader>
                <CardTitle>Latest Strip</CardTitle>
                <CardDescription>
                  {formatFullDate(comic.lastStrip.date)}
                </CardDescription>
              </CardHeader>
              <CardContent>
                {comic.lastStrip.imageUrl && (
                  <Link
                    href={`/comics/${comicId}/read?date=${comic.lastStrip.date}`}
                    className="block bg-canvas rounded-lg overflow-hidden hover:opacity-90 transition-opacity"
                  >
                    <img
                      src={comic.lastStrip.imageUrl}
                      alt={`${comic.name}, ${formatFullDate(comic.lastStrip.date)}`}
                      className="w-full h-auto object-contain"
                    />
                  </Link>
                )}
                <Button asChild className="w-full mt-4">
                  <Link href={`/comics/${comicId}/read?date=${comic.lastStrip.date}`}>Read latest strip</Link>
                </Button>
              </CardContent>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
