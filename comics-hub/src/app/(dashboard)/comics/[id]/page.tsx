'use client';

import { useParams, useRouter } from 'next/navigation';
import { useGetComicQuery } from '@/generated/graphql';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { ArrowLeft, Calendar, User } from 'lucide-react';
import Link from 'next/link';
import { useEffect } from 'react';

export default function ComicDetailPage() {
  const params = useParams();
  const router = useRouter();
  const comicId = parseInt(params.id as string);

  const { data, isLoading, error } = useGetComicQuery({ id: comicId });

  // Redirect to latest strip if available
  useEffect(() => {
    if (data?.comic?.newest) {
      router.replace(`/comics/${comicId}/${data.comic.newest}`);
    }
  }, [data, comicId, router]);

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

  if (error || !data?.comic) {
    return (
      <div className="space-y-6">
        <Card>
          <CardContent className="p-12 text-center">
            <p className="text-ink-subtle mb-4">Failed to load comic details</p>
            <Button onClick={() => router.push('/comics')}>Browse Comics</Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  const comic = data.comic;

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Link href="/comics">
          <Button variant="ghost" size="icon">
            <ArrowLeft className="h-5 w-5" />
          </Button>
        </Link>
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
                  <User className="h-4 w-4" />
                  <span>{comic.author}</span>
                </div>
              )}
              {comic.oldest && comic.newest && (
                <div className="flex items-center gap-2 text-sm text-ink-subtle">
                  <Calendar className="h-4 w-4" />
                  <span>
                    {new Date(comic.oldest).getFullYear()} - {new Date(comic.newest).getFullYear()}
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
                    {new Date(comic.oldest).toLocaleDateString()}
                  </div>
                )}
                {comic.newest && (
                  <div>
                    <span className="font-medium">Latest strip:</span>{' '}
                    {new Date(comic.newest).toLocaleDateString()}
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
                  {new Date(comic.lastStrip.date).toLocaleDateString('en-US', {
                    weekday: 'long',
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric',
                  })}
                </CardDescription>
              </CardHeader>
              <CardContent>
                {comic.lastStrip.imageUrl && (
                  <Link href={`/comics/${comicId}/${comic.lastStrip.date}`}>
                    <div className="aspect-[4/3] bg-canvas rounded-lg overflow-hidden hover:opacity-90 transition-opacity cursor-pointer">
                      <img
                        src={comic.lastStrip.imageUrl}
                        alt={`Latest ${comic.name}`}
                        className="w-full h-full object-cover"
                      />
                    </div>
                  </Link>
                )}
                <Link href={`/comics/${comicId}/${comic.newest}`}>
                  <Button className="w-full mt-4">Read Latest Strip</Button>
                </Link>
              </CardContent>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
