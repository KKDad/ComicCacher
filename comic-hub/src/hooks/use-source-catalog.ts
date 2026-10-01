'use client';

import { useEffect, useMemo, useRef } from 'react';
import { useInfiniteGetSourceCatalogQuery } from '@/generated/graphql';
import type { CatalogEntry, SourceCatalog, SourceComic } from '@/types/sources';

/** The backend caps a catalog page at 500; a catalog is a few hundred comics. */
const PAGE_SIZE = 500;
/** How often to look again while something is being downloaded or refreshed. */
const POLL_MS = 10_000;
/** Stop polling this long after the last sign of background work. */
const POLL_FOR_MS = 5 * 60_000;

function isBusy(source: SourceCatalog | undefined, entries: CatalogEntry[], orphans: SourceComic[]): boolean {
  if (!source) return false;
  const comicBusy = (comic: SourceComic | null | undefined) => !!comic && (comic.avatarPending || comic.startPending);
  return source.refreshing || entries.some((e) => e.thumbnailPending || comicBusy(e.comic)) || orphans.some(comicBusy);
}

/**
 * Loads a source's whole catalog, following the cursor, and keeps polling while
 * thumbnails, avatars or start dates are being fetched in the background.
 */
export function useSourceCatalog(sourceId: string) {
  const busySince = useRef(0);
  const { data, error, isLoading, hasNextPage, isFetchingNextPage, fetchNextPage, refetch } =
    useInfiniteGetSourceCatalogQuery(
      { id: sourceId, first: PAGE_SIZE },
      {
        getNextPageParam: (lastPage) =>
          lastPage.source?.catalog.pageInfo.hasNextPage ? { after: lastPage.source.catalog.pageInfo.endCursor } : undefined,
        initialPageParam: {},
        refetchInterval: () => (Date.now() - busySince.current < POLL_FOR_MS ? POLL_MS : false),
      },
    );

  useEffect(() => {
    if (hasNextPage && !isFetchingNextPage && !error) {
      fetchNextPage();
    }
  }, [hasNextPage, isFetchingNextPage, error, fetchNextPage]);

  const source = data?.pages[0]?.source ?? undefined;
  const entries = useMemo(
    () => data?.pages.flatMap((page) => page.source?.catalog.edges.map((edge) => edge.node) ?? []) ?? [],
    [data],
  );
  const orphans = source?.orphans ?? [];

  const busy = isBusy(source, entries, orphans);
  useEffect(() => {
    if (busy) {
      busySince.current = Date.now();
    }
  }, [busy, data]);

  return {
    source,
    entries,
    orphans,
    error,
    notFound: !isLoading && !error && data !== undefined && !source,
    isLoading: !error && (isLoading || !!hasNextPage),
    /** Call after asking the server for background work, so polling starts at once. */
    markBusy: () => {
      busySince.current = Date.now();
      refetch();
    },
  };
}
