'use client';

import Link from 'next/link';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';

interface FavoriteCardProps {
  comic: {
    id: number;
    name: string;
  };
}

export function FavoriteCard({ comic }: FavoriteCardProps) {
  const initials = comic.name
    .split(' ')
    .map((word) => word[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);

  return (
    <Link
      href={`/comics/${comic.id}`}
      className="flex-shrink-0 w-[140px] group"
    >
      <div className="space-y-2">
        <Avatar className="h-[140px] w-[140px] ring-2 ring-border group-hover:ring-primary transition-all">
          <AvatarFallback className="bg-primary-subtle text-primary text-2xl font-semibold">
            {initials}
          </AvatarFallback>
        </Avatar>
        <p className="text-sm font-medium text-center text-ink truncate group-hover:text-primary transition-colors">
          {comic.name}
        </p>
      </div>
    </Link>
  );
}
