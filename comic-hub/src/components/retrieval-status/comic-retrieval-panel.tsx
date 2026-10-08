import { Card } from '@/components/ui/card';
import { ComicRetrievalDetail, describeComic } from './comic-retrieval-detail';
import type { ComicHealth } from './health';

/**
 * The selected comic's detail beside the grid on tablets and desktops. Sticks under the header so it stays in view while
 * the grid scrolls; the detail is kept short enough to fit, so the panel has no scrollbar of its own.
 */
export function ComicRetrievalPanel({ comic }: { comic: ComicHealth | null }) {
  return (
    <Card
      aria-label="Comic details"
      className="sticky top-[calc(var(--header-height)+1.5rem)] min-w-0 p-5"
    >
      {comic ? (
        <>
          <h2 className="text-lg font-semibold wrap-anywhere text-ink">{comic.comicName}</h2>
          <p className="mb-4 text-sm text-ink-subtle">{describeComic(comic)}</p>
          <ComicRetrievalDetail key={comic.comicId} comic={comic} />
        </>
      ) : (
        <p className="text-sm text-ink-subtle">Select a comic to see its attempts.</p>
      )}
    </Card>
  );
}
