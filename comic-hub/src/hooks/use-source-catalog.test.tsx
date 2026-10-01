import { renderHook } from '@testing-library/react';
import type { InfiniteData } from '@tanstack/react-query';
import { useSourceCatalog } from './use-source-catalog';
import { mockInfiniteQueryResult } from '@/test/mock-query';
import { mockCatalogSource, mockEntry, mockSourceComic } from '@/test/source-fixtures';
import { useInfiniteGetSourceCatalogQuery, type GetSourceCatalogQuery } from '@/generated/graphql';
import type { SourceCatalog } from '@/types/sources';

vi.mock('@/generated/graphql', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/generated/graphql')>()),
  useInfiniteGetSourceCatalogQuery: vi.fn(),
}));

function pages(...sources: (SourceCatalog | null)[]): InfiniteData<GetSourceCatalogQuery> {
  return { pages: sources.map((source) => ({ source })), pageParams: sources.map(() => ({})) };
}

function refetchIntervalOf() {
  const options = vi.mocked(useInfiniteGetSourceCatalogQuery).mock.lastCall?.[1];
  return options?.refetchInterval as () => number | false;
}

describe('useSourceCatalog', () => {
  const fetchNextPage = vi.fn();
  const refetch = vi.fn();

  beforeEach(() => vi.clearAllMocks());

  function mock(data: InfiniteData<GetSourceCatalogQuery> | undefined, extra: Record<string, unknown> = {}) {
    vi.mocked(useInfiniteGetSourceCatalogQuery).mockReturnValue(
      mockInfiniteQueryResult<InfiniteData<GetSourceCatalogQuery>>({ data, isLoading: false, hasNextPage: false, isFetchingNextPage: false, error: null, fetchNextPage, refetch, ...extra }),
    );
  }

  it('flattens every page into one list', () => {
    mock(pages(mockCatalogSource([mockEntry({ identifier: 'a' })]), mockCatalogSource([mockEntry({ identifier: 'b' })])));

    const { result } = renderHook(() => useSourceCatalog('gocomics'));

    expect(result.current.entries.map((e) => e.identifier)).toEqual(['a', 'b']);
    expect(result.current.source?.id).toBe('gocomics');
    expect(result.current.isLoading).toBe(false);
    expect(result.current.notFound).toBe(false);
    expect(refetchIntervalOf()()).toBe(false);
  });

  it('follows the cursor until the last page', () => {
    mock(pages(mockCatalogSource([mockEntry()])), { hasNextPage: true });

    const { result } = renderHook(() => useSourceCatalog('gocomics'));

    expect(fetchNextPage).toHaveBeenCalled();
    expect(result.current.isLoading).toBe(true);
    const getNextPageParam = vi.mocked(useInfiniteGetSourceCatalogQuery).mock.lastCall?.[1].getNextPageParam as (page: GetSourceCatalogQuery) => unknown;
    const more = mockCatalogSource([], { catalog: { totalCount: 900, pageInfo: { hasNextPage: true, endCursor: 'abc' }, edges: [] } });
    expect(getNextPageParam({ source: more })).toEqual({ after: 'abc' });
    expect(getNextPageParam({ source: mockCatalogSource([]) })).toBeUndefined();
  });

  it('reports an unknown source', () => {
    mock(pages(null));

    expect(renderHook(() => useSourceCatalog('nowhere')).result.current.notFound).toBe(true);
  });

  it.each([
    ['a thumbnail is downloading', [mockEntry({ thumbnailPending: true })], [], {}],
    ['an avatar is downloading', [mockEntry({ comic: mockSourceComic({ avatarPending: true }) })], [], {}],
    ['a start is being checked', [], [mockSourceComic({ startPending: true })], {}],
    ['the catalog is being read', [], [], { refreshing: true }],
  ])('polls while %s', (_, entries, orphans, overrides) => {
    mock(pages(mockCatalogSource(entries, { orphans, ...overrides })));

    renderHook(() => useSourceCatalog('gocomics'));

    expect(refetchIntervalOf()()).toBe(10_000);
  });

  it('starts polling when asked to', () => {
    mock(pages(mockCatalogSource([mockEntry()])));
    const { result } = renderHook(() => useSourceCatalog('gocomics'));

    result.current.markBusy();

    expect(refetch).toHaveBeenCalled();
    expect(refetchIntervalOf()()).toBe(10_000);
  });
});
