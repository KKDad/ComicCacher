'use client';

import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet';
import { ComicRetrievalDetail, describeComic } from './comic-retrieval-detail';
import type { ComicHealth } from './health';

interface ComicRetrievalDrawerProps {
  comic: ComicHealth | null;
  onOpenChange: (open: boolean) => void;
}

/** The comic's detail as a slide-over, for phones, where there's no room for the panel. */
export function ComicRetrievalDrawer({ comic, onOpenChange }: ComicRetrievalDrawerProps) {
  return (
    <Sheet open={comic != null} onOpenChange={onOpenChange}>
      <SheetContent className="w-full overflow-y-auto sm:max-w-lg">
        {comic && (
          <>
            <SheetHeader>
              <SheetTitle>{comic.comicName}</SheetTitle>
              <SheetDescription>{describeComic(comic)}</SheetDescription>
            </SheetHeader>
            <div className="px-4 pb-6">
              <ComicRetrievalDetail key={comic.comicId} comic={comic} />
            </div>
          </>
        )}
      </SheetContent>
    </Sheet>
  );
}
