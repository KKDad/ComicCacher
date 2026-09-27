'use client';

import Link from 'next/link';
import { Heart } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { ImageWithFallback } from '@/components/ui/image-with-fallback';
import { formatShortDate } from '@/lib/date-utils';

interface ComicTileProps {
  comic: {
    id: number;
    name: string;
    date: string;
    thumbnail?: string;
  };
  isNew?: boolean;
  isFavorite?: boolean;
  onToggleFavorite?: (e: React.MouseEvent) => void;
}

/**
 * The title link stretches over the whole card (after:absolute inset-0), so
 * the card is one click target while the favorite button stays a sibling of
 * the link instead of an illegal button-inside-anchor.
 */
export function ComicTile({ comic, isNew, isFavorite, onToggleFavorite }: ComicTileProps) {
  const formattedDate = formatShortDate(comic.date);

  return (
    <Card className="relative overflow-hidden py-0 gap-0 hover:shadow-md transition-shadow group focus-within:ring-[3px] focus-within:ring-ring/50">
      <div className="aspect-[4/3] bg-canvas overflow-hidden">
        <ImageWithFallback
          src={comic.thumbnail}
          alt=""
          fallbackText={comic.name[0]}
          className="strip-image motion-safe:group-hover:scale-105 transition-transform"
        />
      </div>
      <CardContent className="p-3">
        <h3 className="font-sans font-medium text-ink truncate group-hover:text-primary transition-colors">
          <Link
            href={`/comics/${comic.id}/read?date=${comic.date}`}
            className="outline-none after:absolute after:inset-0"
          >
            {comic.name}
          </Link>
        </h3>
        <div className="flex items-center justify-between mt-1">
          <p className="text-sm text-ink-subtle">{formattedDate}</p>
          {isNew && (
            <Badge className="text-xs bg-highlight text-on-highlight border-transparent">
              New
            </Badge>
          )}
        </div>
      </CardContent>
      {onToggleFavorite && (
        <button
          type="button"
          aria-label={isFavorite ? `Remove ${comic.name} from favorites` : `Add ${comic.name} to favorites`}
          aria-pressed={isFavorite}
          className="absolute top-2 right-2 z-10 flex h-10 w-10 items-center justify-center rounded-full bg-black/50 hover:bg-black/70 transition-colors"
          onClick={onToggleFavorite}
        >
          <Heart
            aria-hidden="true"
            className={`h-5 w-5 ${isFavorite ? 'fill-favorite text-favorite' : 'text-white'}`}
          />
        </button>
      )}
    </Card>
  );
}
