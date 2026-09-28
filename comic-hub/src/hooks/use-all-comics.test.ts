import { renderHook } from '@testing-library/react';
import { useAllComics } from './use-all-comics';
import type { InfiniteData, UseInfiniteQueryResult } from '@tanstack/react-query';
import { useInfiniteGetComicsQuery, type GetComicsQuery } from '@/generated/graphql';
import { mockInfiniteQueryResult } from '@/test/mock-query';

vi.mock('@/generated/graphql', () => ({
  useInfiniteGetComicsQuery: vi.fn(),
}));

function page(ids: number[], hasNextPage: boolean, endCursor: string | null = null): GetComicsQuery {
  return {
    comics: {
      totalCount: ids.length,
      edges: ids.map((id) => ({
        cursor: `c${id}`,
        node: { id, name: `Comic ${id}`, description: null, oldest: null, newest: null, avatarUrl: null, lastStrip: null },
      })),
      pageInfo: { hasNextPage, hasPreviousPage: false, startCursor: null, endCursor },
    },
  };
}

function pages(...loaded: GetComicsQuery[]): InfiniteData<GetComicsQuery> {
  return { pages: loaded, pageParams: loaded.map(() => ({})) };
}

type ComicsResult = UseInfiniteQueryResult<InfiniteData<GetComicsQuery>>;

function mockQuery(overrides: Partial<ComicsResult>) {
  const fetchNextPage = vi.fn();
  vi.mocked(useInfiniteGetComicsQuery).mockReturnValue(mockInfiniteQueryResult({
    data: undefined,
    error: null,
    isLoading: false,
    hasNextPage: false,
    isFetchingNextPage: false,
    fetchNextPage,
    ...overrides,
  }));
  return fetchNextPage;
}

// The options useAllComics passed to the generated hook
function queryOptions() {
  return vi.mocked(useInfiniteGetComicsQuery).mock.calls[0][1];
}

describe('useAllComics', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('requests the backend maximum page size', () => {
    mockQuery({});
    renderHook(() => useAllComics());
    expect(vi.mocked(useInfiniteGetComicsQuery).mock.calls[0][0]).toEqual({ first: 50 });
  });

  it('builds the next page param from the end cursor', () => {
    mockQuery({});
    renderHook(() => useAllComics());
    const { getNextPageParam } = queryOptions();
    expect(getNextPageParam(page([1], true, 'c1'), [], {}, [])).toEqual({ after: 'c1' });
    expect(getNextPageParam(page([1], false), [], {}, [])).toBeUndefined();
  });

  it('keeps fetching while more pages remain and reports loading', () => {
    const fetchNextPage = mockQuery({ data: pages(page([1, 2], true, 'c1')), hasNextPage: true });
    const { result } = renderHook(() => useAllComics());
    expect(fetchNextPage).toHaveBeenCalled();
    expect(result.current.isLoading).toBe(true);
  });

  it('does not double-fetch while a page is in flight', () => {
    const fetchNextPage = mockQuery({ hasNextPage: true, isFetchingNextPage: true });
    renderHook(() => useAllComics());
    expect(fetchNextPage).not.toHaveBeenCalled();
  });

  it('flattens every page once complete', () => {
    mockQuery({ data: pages(page([1, 2], true, 'c1'), page([3], false)) });
    const { result } = renderHook(() => useAllComics());
    expect(result.current.comics.map((c) => c.id)).toEqual([1, 2, 3]);
    expect(result.current.isLoading).toBe(false);
  });

  it('stops and surfaces the error when a page fails', () => {
    const error = new Error('boom');
    const fetchNextPage = mockQuery({ hasNextPage: true, error });
    const { result } = renderHook(() => useAllComics());
    expect(fetchNextPage).not.toHaveBeenCalled();
    expect(result.current.error).toBe(error);
    expect(result.current.isLoading).toBe(false);
  });
});
