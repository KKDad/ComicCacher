'use client';

import Link from 'next/link';
import { Heart } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { FavoriteCard } from '@/components/comics/favorite-card';

interface FavoriteComic {
  id: number;
  name: string;
  avatarUrl?: string | null;
}

interface FavoritesSectionProps {
  favorites?: FavoriteComic[] | null;
  isLoading?: boolean;
}

// Every state is the height of a row of favorites, so adding the first favorite
// doesn't move the cards below (and the next click doesn't land on the wrong one).
const ROW_HEIGHT = 'min-h-[11.5rem]';

function Heading() {
  return <h2 className="text-xl font-semibold mb-4 text-ink">Your Favorites</h2>;
}

export function FavoritesSection({ favorites = null, isLoading = false }: FavoritesSectionProps) {
  if (isLoading) {
    return (
      <section aria-busy="true">
        <Heading />
        <div className={`flex gap-4 overflow-x-auto pb-4 ${ROW_HEIGHT}`}>
          {[...Array(5)].map((_, i) => (
            <div key={i} className="flex-shrink-0">
              <Skeleton className="h-[140px] w-[140px] rounded-full mb-2" />
              <Skeleton className="h-4 w-24 mx-auto" />
            </div>
          ))}
        </div>
      </section>
    );
  }

  if (!favorites || favorites.length === 0) {
    return (
      <section>
        <Heading />
        <Card className={`${ROW_HEIGHT} border-dashed flex-row items-center justify-center gap-4 p-6 text-center sm:text-left`}>
          <Heart className="h-10 w-10 shrink-0 text-ink-muted" aria-hidden="true" />
          <div>
            <p className="font-medium text-ink">No favorite comics yet</p>
            <p className="text-sm text-ink-subtle">Tap the heart on any comic to add it here and to Today</p>
          </div>
          <Button asChild className="shrink-0">
            <Link href="/comics">Browse comics</Link>
          </Button>
        </Card>
      </section>
    );
  }

  return (
    <section>
      <Heading />
      <div className={`flex gap-4 overflow-x-auto pb-4 -mx-4 px-4 ${ROW_HEIGHT}`}>
        {favorites.map((comic) => (
          <FavoriteCard key={comic.id} comic={comic} />
        ))}
      </div>
    </section>
  );
}
