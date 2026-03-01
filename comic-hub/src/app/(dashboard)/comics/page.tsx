'use client';

import { useState, useEffect } from 'react';
import { ComicTile } from '@/components/comics/comic-tile';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { BookOpen, Loader2 } from 'lucide-react';
import { useGetComicsQuery } from '@/generated/graphql';
import type { GetComicsQuery } from '@/generated/graphql';

type ComicNode = GetComicsQuery['comics']['edges'][number]['node'];

export default function ComicsPage() {
  const [comics, setComics] = useState<ComicNode[]>([]);
  const [afterCursor, setAfterCursor] = useState<string | undefined>(undefined);
  const [hasNextPage, setHasNextPage] = useState(false);

  const { data, isLoading, isFetching } = useGetComicsQuery(
    { first: 20, after: afterCursor },
  );

  useEffect(() => {
    if (data?.comics) {
      const newNodes = data.comics.edges.map((e) => e.node);
      setComics((prev) =>
        afterCursor ? [...prev, ...newNodes] : newNodes,
      );
      setHasNextPage(data.comics.pageInfo.hasNextPage);
    }
  }, [data, afterCursor]);

  if (isLoading && comics.length === 0) {
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

  if (!isLoading && comics.length === 0) {
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
            Explore all {comics.length} available comics
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

      {hasNextPage && (
        <div className="flex justify-center pt-4">
          <Button
            variant="outline"
            disabled={isFetching}
            onClick={() => {
              const endCursor = data?.comics.pageInfo.endCursor;
              if (endCursor) {
                setAfterCursor(endCursor);
              }
            }}
          >
            {isFetching ? (
              <>
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                Loading...
              </>
            ) : (
              'Load More'
            )}
          </Button>
        </div>
      )}
    </div>
  );
}
