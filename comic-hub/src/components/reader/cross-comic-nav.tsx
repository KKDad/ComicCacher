'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { useReadingList } from '@/hooks/use-reading-list';

interface CrossComicNavProps {
  comicId: number;
}

export function CrossComicNav({ comicId }: CrossComicNavProps) {
  const { previousComic, nextComic, navigateToComic } = useReadingList(comicId);
  const [hoverSide, setHoverSide] = useState<'left' | 'right' | null>(null);

  if (!previousComic && !nextComic) return null;

  return (
    <>
      {/* Left edge — previous comic */}
      {previousComic && (
        <div
          className="fixed left-0 top-1/2 -translate-y-1/2 z-sticky"
          onMouseEnter={() => setHoverSide('left')}
          onMouseLeave={() => setHoverSide(null)}
        >
          <Button
            variant="ghost"
            size="icon"
            onClick={() => navigateToComic(previousComic.id)}
            className={`h-20 w-10 rounded-l-none bg-card/60 hover:bg-muted text-ink-subtle hover:text-ink transition-opacity ${hoverSide === 'left' ? 'opacity-100' : 'opacity-0'}`}
            aria-label={`Previous comic: ${previousComic.name}`}
            title={previousComic.name}
          >
            <ChevronLeft className="h-5 w-5" />
          </Button>
        </div>
      )}

      {/* Right edge — next comic */}
      {nextComic && (
        <div
          className="fixed right-0 top-1/2 -translate-y-1/2 z-sticky"
          onMouseEnter={() => setHoverSide('right')}
          onMouseLeave={() => setHoverSide(null)}
        >
          <Button
            variant="ghost"
            size="icon"
            onClick={() => navigateToComic(nextComic.id)}
            className={`h-20 w-10 rounded-r-none bg-card/60 hover:bg-muted text-ink-subtle hover:text-ink transition-opacity ${hoverSide === 'right' ? 'opacity-100' : 'opacity-0'}`}
            aria-label={`Next comic: ${nextComic.name}`}
            title={nextComic.name}
          >
            <ChevronRight className="h-5 w-5" />
          </Button>
        </div>
      )}
    </>
  );
}
