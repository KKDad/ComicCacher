'use client';

import { useReadingList } from '@/hooks/use-reading-list';
import { Button } from '@/components/ui/button';
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from '@/components/ui/sheet';
import { List } from 'lucide-react';

interface ReadingListDrawerProps {
  comicId: number;
}

export function ReadingListDrawer({ comicId }: ReadingListDrawerProps) {
  const { comics, navigateToComic, isLoading } = useReadingList(comicId);

  return (
    <Sheet>
      <SheetTrigger asChild>
        <Button
          variant="ghost"
          size="icon"
          aria-label="Reading list"
          className="text-ink-subtle hover:text-ink hover:bg-muted"
        >
          <List className="h-5 w-5" />
        </Button>
      </SheetTrigger>
      <SheetContent side="bottom" className="bg-canvas border-border max-h-[60vh]">
        <SheetHeader>
          <SheetTitle className="text-ink">Reading List</SheetTitle>
        </SheetHeader>
        <div className="overflow-y-auto mt-4 -mx-2">
          {isLoading ? (
            <p className="text-sm text-ink-muted text-center py-4">Loading...</p>
          ) : comics.length === 0 ? (
            <p className="text-sm text-ink-muted text-center py-4">No comics in reading list</p>
          ) : (
            <ul className="space-y-1">
              {comics.map((comic) => (
                <li key={comic.id}>
                  <button
                    onClick={() => navigateToComic(comic.id)}
                    className={`w-full flex items-center gap-3 px-3 py-2 rounded-md text-left transition-colors hover:bg-muted ${comic.id === comicId ? 'bg-muted' : ''}`}
                  >
                    {comic.avatarUrl ? (
                      <img
                        src={comic.avatarUrl}
                        alt=""
                        className="h-8 w-8 rounded-full object-cover flex-shrink-0"
                      />
                    ) : (
                      <div className="h-8 w-8 rounded-full bg-muted flex items-center justify-center flex-shrink-0">
                        <span className="text-xs font-medium text-ink-subtle">
                          {comic.name[0]}
                        </span>
                      </div>
                    )}
                    <span className="text-sm text-ink truncate flex-1">
                      {comic.name}
                    </span>
                    {comic.hasUnread && (
                      <span className="h-2 w-2 rounded-full bg-primary flex-shrink-0" />
                    )}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      </SheetContent>
    </Sheet>
  );
}
