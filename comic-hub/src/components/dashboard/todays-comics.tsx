'use client';

import Link from 'next/link';
import { Calendar } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { ComicTile } from '@/components/comics/comic-tile';
import { EmptyState } from '@/components/ui/empty-state';

interface TodaysComic {
  id: number;
  name: string;
  date: string;
  thumbnail?: string;
  isNew?: boolean;
  isFavorite?: boolean;
  onToggleFavorite?: (e: React.MouseEvent) => void;
}

interface TodaysComicsProps {
  comics?: TodaysComic[] | null;
  isLoading?: boolean;
}

export function TodaysComics({ comics = null, isLoading = false }: TodaysComicsProps) {
  const today = new Date().toLocaleDateString('en-US', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  if (isLoading) {
    return (
      <section>
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-xl font-semibold text-ink">Today's Comics</h2>
            <p className="text-sm text-ink-subtle">{today}</p>
          </div>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {[...Array(8)].map((_, i) => (
            <Card key={i} className="overflow-hidden">
              <Skeleton className="aspect-[4/3] w-full" />
              <div className="p-3 space-y-2">
                <Skeleton className="h-5 w-3/4" />
                <Skeleton className="h-4 w-1/2" />
              </div>
            </Card>
          ))}
        </div>
      </section>
    );
  }

  if (!comics || comics.length === 0) {
    return (
      <section>
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-xl font-semibold text-ink">Today's Comics</h2>
            <p className="text-sm text-ink-subtle">{today}</p>
          </div>
        </div>
        <EmptyState
          icon={Calendar}
          title="No comics for today"
          description="Check back later or browse the archive"
          actionLabel="View Archive"
          actionHref="/comics"
        />
      </section>
    );
  }

  return (
    <section>
      <div className="flex items-center justify-between mb-4">
        <div>
          <h2 className="text-xl font-semibold text-ink">Today's Comics</h2>
          <p className="text-sm text-ink-subtle">{today}</p>
        </div>
        <Link href="/comics">
          <Button variant="outline" size="sm">
            View All
          </Button>
        </Link>
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {comics.map((comic) => (
          <ComicTile
            key={comic.id}
            comic={comic}
            isNew={comic.isNew}
            isFavorite={comic.isFavorite}
            onToggleFavorite={comic.onToggleFavorite}
          />
        ))}
      </div>
    </section>
  );
}
