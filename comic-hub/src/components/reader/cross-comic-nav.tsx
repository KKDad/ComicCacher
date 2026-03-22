'use client';

import { Button } from '@/components/ui/button';
import { ChevronLeft, ChevronRight } from 'lucide-react';

interface CrossComicNavComic {
  id: number;
  name: string;
}

interface CrossComicNavProps {
  previousComic: CrossComicNavComic | null;
  nextComic: CrossComicNavComic | null;
  navigateToComic: (comicId: number) => void;
}

export function CrossComicNav({ previousComic, nextComic, navigateToComic }: CrossComicNavProps) {
  if (!previousComic && !nextComic) return null;

  return (
    <div className="fixed top-1/2 -translate-y-1/2 z-sticky left-1/2 -translate-x-1/2 w-full max-w-3xl pointer-events-none px-4">
      <div className="relative">
        {/* Left gutter — previous comic */}
        {previousComic && (
          <div className="absolute -left-16 top-0 -translate-y-1/2 pointer-events-auto">
            <Button
              variant="ghost"
              size="icon"
              onClick={() => navigateToComic(previousComic.id)}
              className="h-20 w-12 rounded-md bg-card/80 hover:bg-muted text-ink-subtle hover:text-ink opacity-40 hover:opacity-100 transition-opacity"
              aria-label={`Previous comic: ${previousComic.name}`}
              title={previousComic.name}
            >
              <ChevronLeft className="h-6 w-6" />
            </Button>
          </div>
        )}

        {/* Right gutter — next comic */}
        {nextComic && (
          <div className="absolute -right-16 top-0 -translate-y-1/2 pointer-events-auto">
            <Button
              variant="ghost"
              size="icon"
              onClick={() => navigateToComic(nextComic.id)}
              className="h-20 w-12 rounded-md bg-card/80 hover:bg-muted text-ink-subtle hover:text-ink opacity-40 hover:opacity-100 transition-opacity"
              aria-label={`Next comic: ${nextComic.name}`}
              title={nextComic.name}
            >
              <ChevronRight className="h-6 w-6" />
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}
