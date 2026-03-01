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

export function FavoritesSection({ favorites = null, isLoading = false }: FavoritesSectionProps) {
  if (isLoading) {
    return (
      <section>
        <h2 className="text-xl font-semibold mb-4 text-ink">Your Favorites</h2>
        <div className="flex gap-4 overflow-x-auto pb-4">
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
        <h2 className="text-xl font-semibold mb-4 text-ink">Your Favorites</h2>
        <Card className="border-dashed">
          <div className="flex flex-col items-center justify-center p-12 text-center">
            <Heart className="h-12 w-12 text-ink-muted mb-4" />
            <p className="text-ink-subtle mb-2">No favorite comics yet</p>
            <p className="text-sm text-ink-muted mb-4">
              Mark comics as favorites to see them here
            </p>
            <Link href="/comics">
              <Button>Browse Comics</Button>
            </Link>
          </div>
        </Card>
      </section>
    );
  }

  return (
    <section>
      <h2 className="text-xl font-semibold mb-4 text-ink">Your Favorites</h2>
      <div className="flex gap-4 overflow-x-auto pb-4 -mx-4 px-4">
        {favorites.map((comic) => (
          <FavoriteCard key={comic.id} comic={comic} />
        ))}
      </div>
    </section>
  );
}
