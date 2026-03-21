'use client';

import Link from 'next/link';
import { Heart } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';

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

export function ComicTile({ comic, isNew, isFavorite, onToggleFavorite }: ComicTileProps) {
  const formattedDate = new Date(comic.date).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
  });

  return (
    <Link href={`/comics/${comic.id}/read?date=${comic.date}`}>
      <Card className="overflow-hidden hover:shadow-md transition-shadow group">
        <div className="aspect-[4/3] bg-canvas overflow-hidden relative">
          {comic.thumbnail ? (
            <img
              src={comic.thumbnail}
              alt={comic.name}
              className="w-full h-full object-cover group-hover:scale-105 transition-transform"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-ink-muted">
              {comic.name[0]}
            </div>
          )}
          {onToggleFavorite && (
            <button
              type="button"
              aria-label={isFavorite ? 'Remove from favorites' : 'Add to favorites'}
              className="absolute top-2 right-2 p-1.5 rounded-full bg-black/40 hover:bg-black/60 transition-colors"
              onClick={(e) => {
                e.preventDefault();
                e.stopPropagation();
                onToggleFavorite(e);
              }}
            >
              <Heart
                className={`h-4 w-4 ${isFavorite ? 'fill-red-500 text-red-500' : 'text-white'}`}
              />
            </button>
          )}
        </div>
        <CardContent className="p-3">
          <h3 className="font-medium text-ink truncate group-hover:text-primary transition-colors">
            {comic.name}
          </h3>
          <div className="flex items-center justify-between mt-1">
            <p className="text-sm text-ink-subtle">{formattedDate}</p>
            {isNew && (
              <Badge variant="secondary" className="text-xs">
                New
              </Badge>
            )}
          </div>
        </CardContent>
      </Card>
    </Link>
  );
}
