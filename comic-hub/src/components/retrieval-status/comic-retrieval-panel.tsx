import { Card } from '@/components/ui/card';
import { ComicRetrievalDetail, describeComic } from './comic-retrieval-detail';
import type { ComicHealth } from './health';

/**
 * The selected comic's detail beside the grid on tablets and desktops. Sticks under the header and scrolls on its own, so
 * it stays in view while the grid scrolls.
 */
export function ComicRetrievalPanel({ comic }: { comic: ComicHealth | null }) {
  return (
    <Card
      aria-label="Comic details"
      className="sticky top-[calc(var(--header-height)+1.5rem)] max-h-[calc(100dvh-var(--header-height)-3rem)] overflow-y-auto p-5"
    >
      {comic ? (
        <>
          <h2 className="text-lg font-semibold text-ink">{comic.comicName}</h2>
          <p className="mb-4 text-sm text-ink-subtle">{describeComic(comic)}</p>
          <ComicRetrievalDetail comic={comic} />
        </>
      ) : (
        <p className="text-sm text-ink-subtle">Select a comic to see its attempts.</p>
      )}
    </Card>
  );
}
