'use client';

import Link from 'next/link';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';

interface ComicTileProps {
  comic: {
    id: number;
    name: string;
    date: string;
    thumbnail?: string;
  };
}

export function ComicTile({ comic }: ComicTileProps) {
  const formattedDate = new Date(comic.date).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
  });

  return (
    <Link href={`/comics/${comic.id}/${comic.date}`}>
      <Card className="overflow-hidden hover:shadow-md transition-shadow group">
        <div className="aspect-[4/3] bg-canvas overflow-hidden">
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
        </div>
        <CardContent className="p-3">
          <h3 className="font-medium text-ink truncate group-hover:text-primary transition-colors">
            {comic.name}
          </h3>
          <div className="flex items-center justify-between mt-1">
            <p className="text-sm text-ink-subtle">{formattedDate}</p>
            <Badge variant="secondary" className="text-xs">
              New
            </Badge>
          </div>
        </CardContent>
      </Card>
    </Link>
  );
}
