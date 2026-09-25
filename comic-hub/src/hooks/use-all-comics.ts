'use client';

import { useEffect, useMemo } from 'react';
import { useInfiniteGetComicsQuery } from '@/generated/graphql';

/** The backend caps `comics(first:)` at 50. */
const PAGE_SIZE = 50;

/**
 * Loads every comic by following the connection's cursor until the last page.
 * Use it wherever a view must cover the whole catalogue (favorites, reading
 * list, latest updates); a single `first: N` page silently drops the rest.
 */
export function useAllComics() {
  const { data, error, isLoading, hasNextPage, isFetchingNextPage, fetchNextPage } =
    useInfiniteGetComicsQuery(
      { first: PAGE_SIZE },
      {
        getNextPageParam: (lastPage) =>
          lastPage.comics.pageInfo.hasNextPage
            ? { after: lastPage.comics.pageInfo.endCursor }
            : undefined,
        initialPageParam: {},
        staleTime: 5 * 60 * 1000,
      },
    );

  useEffect(() => {
    if (hasNextPage && !isFetchingNextPage && !error) {
      fetchNextPage();
    }
  }, [hasNextPage, isFetchingNextPage, error, fetchNextPage]);

  const comics = useMemo(
    () => data?.pages.flatMap((page) => page.comics.edges.map((edge) => edge.node)) ?? [],
    [data],
  );

  return {
    comics,
    error,
    // Partial lists would make favorites flicker in one page at a time.
    isLoading: !error && (isLoading || !!hasNextPage),
  };
}
