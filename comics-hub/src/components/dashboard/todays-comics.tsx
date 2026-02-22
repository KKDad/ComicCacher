'use client';

import { Calendar } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { ComicTile } from '@/components/comics/comic-tile';

interface TodaysComic {
  id: number;
  name: string;
  date: string;
  thumbnail?: string;
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
        <Card className="border-dashed">
          <div className="flex flex-col items-center justify-center p-12 text-center">
            <Calendar className="h-12 w-12 text-ink-muted mb-4" />
            <p className="text-ink-subtle mb-2">No comics for today</p>
            <p className="text-sm text-ink-muted mb-4">
              Check back later or browse the archive
            </p>
            <Button>View Archive</Button>
          </div>
        </Card>
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
        <Button variant="outline" size="sm">
          View All
        </Button>
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {comics.map((comic) => (
          <ComicTile key={comic.id} comic={comic} />
        ))}
      </div>
    </section>
  );
}
