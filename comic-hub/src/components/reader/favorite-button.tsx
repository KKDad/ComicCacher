'use client';

import { Heart } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useFavorite } from '@/hooks/use-favorite';
import { useHydrated } from '@/hooks/use-hydrated';
import { cn } from '@/lib/utils';

interface FavoriteButtonProps {
  comicId: number;
  comicName: string;
}

export function FavoriteButton({ comicId, comicName }: FavoriteButtonProps) {
  const favorite = useFavorite(comicId);
  // The server can't know the user's favorites; render "not a favorite" until hydrated
  const hydrated = useHydrated();
  const isFavorite = hydrated && favorite.isFavorite;
  const { toggle, isPending } = favorite;
  const label = isFavorite ? `Remove ${comicName} from favorites` : `Add ${comicName} to favorites`;

  return (
    <Button
      variant="ghost"
      size="icon"
      onClick={toggle}
      disabled={isPending}
      aria-label={label}
      aria-pressed={isFavorite}
      title={label}
      className={cn(
        'h-11 w-11 rounded-full border border-border hover:bg-muted',
        isFavorite ? 'text-error hover:text-error' : 'text-ink-subtle hover:text-ink',
      )}
    >
      <Heart className={cn('h-5 w-5', isFavorite && 'fill-current')} />
    </Button>
  );
}
