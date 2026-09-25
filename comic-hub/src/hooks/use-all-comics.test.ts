import { renderHook } from '@testing-library/react';
import { useAllComics } from './use-all-comics';
import { useInfiniteGetComicsQuery } from '@/generated/graphql';

vi.mock('@/generated/graphql', () => ({
  useInfiniteGetComicsQuery: vi.fn(),
}));

function page(ids: number[], hasNextPage: boolean, endCursor: string | null = null) {
  return {
    comics: {
      edges: ids.map((id) => ({ node: { id, name: `Comic ${id}` } })),
      pageInfo: { hasNextPage, endCursor },
    },
  };
}

function mockQuery(overrides: Record<string, unknown>) {
  const fetchNextPage = vi.fn();
  vi.mocked(useInfiniteGetComicsQuery).mockReturnValue({
    data: undefined,
    error: null,
    isLoading: false,
    hasNextPage: false,
    isFetchingNextPage: false,
    fetchNextPage,
    ...overrides,
  } as any);
  return fetchNextPage;
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
    const { getNextPageParam } = vi.mocked(useInfiniteGetComicsQuery).mock.calls[0][1] as any;
    expect(getNextPageParam(page([1], true, 'c1'))).toEqual({ after: 'c1' });
    expect(getNextPageParam(page([1], false))).toBeUndefined();
  });

  it('keeps fetching while more pages remain and reports loading', () => {
    const fetchNextPage = mockQuery({ data: { pages: [page([1, 2], true, 'c1')] }, hasNextPage: true });
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
    mockQuery({ data: { pages: [page([1, 2], true, 'c1'), page([3], false)] } });
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
