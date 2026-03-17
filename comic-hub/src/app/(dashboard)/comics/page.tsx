'use client';

import { useRef, useEffect, useMemo } from 'react';
import { ComicTile } from '@/components/comics/comic-tile';
import { Card } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { BookOpen, Loader2 } from 'lucide-react';
import { useInfiniteGetComicsQuery } from '@/generated/graphql';

const PAGE_SIZE = 20;

export default function ComicsPage() {
  const sentinelRef = useRef<HTMLDivElement>(null);

  const { data, isLoading, isFetchingNextPage, hasNextPage, fetchNextPage } =
    useInfiniteGetComicsQuery(
      { first: PAGE_SIZE },
      {
        getNextPageParam: (lastPage) =>
          lastPage.comics.pageInfo.hasNextPage
            ? { after: lastPage.comics.pageInfo.endCursor }
            : undefined,
        initialPageParam: {},
      },
    );

  useEffect(() => {
    const sentinel = sentinelRef.current;
    if (!sentinel) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasNextPage && !isFetchingNextPage) {
          fetchNextPage();
        }
      },
      { rootMargin: '200px' },
    );

    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  const comics = useMemo(
    () => data?.pages.flatMap((page) => page.comics.edges.map((e) => e.node)) ?? [],
    [data],
  );

  const totalCount = data?.pages[0]?.comics.totalCount ?? 0;

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <Skeleton className="h-8 w-48 mb-2" />
            <Skeleton className="h-5 w-64" />
          </div>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {[...Array(12)].map((_, i) => (
            <Card key={i} className="overflow-hidden">
              <Skeleton className="aspect-[4/3] w-full" />
              <div className="p-3 space-y-2">
                <Skeleton className="h-5 w-3/4" />
                <Skeleton className="h-4 w-1/2" />
              </div>
            </Card>
          ))}
        </div>
      </div>
    );
  }

  if (comics.length === 0) {
    return (
      <div className="space-y-6">
        <h1 className="text-3xl font-bold text-ink">Browse Comics</h1>
        <Card className="border-dashed">
          <div className="flex flex-col items-center justify-center p-12 text-center">
            <BookOpen className="h-12 w-12 text-ink-muted mb-4" />
            <p className="text-ink-subtle mb-2">No comics available</p>
            <p className="text-sm text-ink-muted">
              Check back later when comics are added
            </p>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-ink">Browse Comics</h1>
          <p className="text-ink-subtle mt-1">
            Explore all {totalCount} available comics
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {comics.map((comic) => (
          <ComicTile
            key={comic.id}
            comic={{
              id: comic.id,
              name: comic.name,
              date: comic.lastStrip?.date ?? comic.newest,
              thumbnail: comic.lastStrip?.imageUrl ?? comic.avatarUrl ?? undefined,
            }}
          />
        ))}
      </div>

      <div ref={sentinelRef} className="h-1" />

      {isFetchingNextPage && (
        <div className="flex justify-center py-4">
          <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
        </div>
      )}
    </div>
  );
}
