'use client';

import Link from 'next/link';
import { Calendar } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { ComicTile } from '@/components/comics/comic-tile';
import { EmptyState } from '@/components/ui/empty-state';

interface LatestComic {
  id: number;
  name: string;
  date: string;
  thumbnail?: string;
  isNew?: boolean;
  isFavorite?: boolean;
  onToggleFavorite?: (e: React.MouseEvent) => void;
}

interface LatestUpdatesProps {
  comics?: LatestComic[] | null;
  isLoading?: boolean;
}

function SectionHeading({ showViewAll = false }: { showViewAll?: boolean }) {
  return (
    <div className="flex items-center justify-between mb-4">
      <div>
        <h2 className="text-xl font-semibold text-ink">Latest Updates</h2>
        <p className="text-sm text-ink-subtle">The newest strips across your comics</p>
      </div>
      {showViewAll && (
        <Button asChild variant="outline" size="sm">
          <Link href="/comics">View all</Link>
        </Button>
      )}
    </div>
  );
}

export function LatestUpdates({ comics = null, isLoading = false }: LatestUpdatesProps) {
  if (isLoading) {
    return (
      <section aria-busy="true">
        <SectionHeading />
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
        <SectionHeading />
        <EmptyState
          icon={Calendar}
          title="No new strips yet"
          description="Check back later or browse the archive"
          actionLabel="Browse comics"
          actionHref="/comics"
        />
      </section>
    );
  }

  return (
    <section>
      <SectionHeading showViewAll />
      <ul className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {comics.map((comic) => (
          <li key={comic.id}>
            <ComicTile
              comic={comic}
              isNew={comic.isNew}
              isFavorite={comic.isFavorite}
              onToggleFavorite={comic.onToggleFavorite}
            />
          </li>
        ))}
      </ul>
    </section>
  );
}
