'use client';

import { useGetComicsQuery } from '@/generated/graphql';
import { ComicTile } from '@/components/comics/comic-tile';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { BookOpen } from 'lucide-react';
import { useMemo, useState } from 'react';

export default function ComicsPage() {
  const [first] = useState(100);
  const { data, isLoading, error } = useGetComicsQuery({ first });

  const comics = useMemo(() => {
    if (!data?.comics?.edges) return [];

    return data.comics.edges.map((edge) => ({
      id: edge.node.id,
      name: edge.node.name,
      date: edge.node.lastStrip?.date || edge.node.newest || new Date().toISOString().split('T')[0],
      thumbnail: edge.node.lastStrip?.imageUrl || undefined,
    }));
  }, [data]);

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

  if (error) {
    return (
      <div className="space-y-6">
        <h1 className="text-3xl font-bold text-ink">Browse Comics</h1>
        <Card>
          <div className="flex flex-col items-center justify-center p-12 text-center">
            <p className="text-ink-subtle mb-4">Failed to load comics</p>
            <Button onClick={() => window.location.reload()}>Retry</Button>
          </div>
        </Card>
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
            Explore all {comics.length} available comics
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {comics.map((comic) => (
          <ComicTile key={comic.id} comic={comic} />
        ))}
      </div>

      {data?.comics?.pageInfo?.hasNextPage && (
        <div className="flex justify-center pt-4">
          <Button variant="outline">Load More</Button>
        </div>
      )}
    </div>
  );
}
