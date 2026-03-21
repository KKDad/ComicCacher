import { renderHook } from '@testing-library/react';
import { useReadingList } from './use-reading-list';
import { useGetComicsQuery, useGetUserPreferencesQuery } from '@/generated/graphql';
import { usePreferencesStore } from '@/stores/preferences-store';
import { useRouter } from 'next/navigation';

vi.mock('@/generated/graphql', () => ({
  useGetComicsQuery: vi.fn(),
  useGetUserPreferencesQuery: vi.fn(),
}));

const mockPush = vi.fn();

describe('useReadingList', () => {
  beforeEach(() => {
    vi.mocked(useRouter).mockReturnValue({
      push: mockPush,
      replace: vi.fn(),
      prefetch: vi.fn(),
      back: vi.fn(),
      refresh: vi.fn(),
      forward: vi.fn(),
    });
    usePreferencesStore.setState({
      settings: { readerNavMode: 'all' } as any,
      isHydrated: true,
    });
    mockPush.mockClear();
  });

  function mockComicsAndPrefs(
    comics: Array<{ id: number; name: string; newest?: string; avatarUrl?: string }>,
    lastReadDates: Array<{ comicId: number; date: string }> = [],
    favoriteComics: number[] = [],
  ) {
    vi.mocked(useGetComicsQuery).mockReturnValue({
      data: {
        comics: {
          edges: comics.map((c) => ({
            node: { ...c, avatarUrl: c.avatarUrl ?? null, newest: c.newest ?? null },
          })),
        },
      },
      isLoading: false,
    } as any);

    vi.mocked(useGetUserPreferencesQuery).mockReturnValue({
      data: {
        preferences: { lastReadDates, favoriteComics },
      },
      isLoading: false,
    } as any);
  }

  it('returns sorted list of comics', () => {
    mockComicsAndPrefs([
      { id: 1, name: 'Zits' },
      { id: 2, name: 'Garfield' },
      { id: 3, name: 'Archie' },
    ]);

    const { result } = renderHook(() => useReadingList(1));

    expect(result.current.comics).toHaveLength(3);
    expect(result.current.comics[0].name).toBe('Archie');
    expect(result.current.comics[1].name).toBe('Garfield');
    expect(result.current.comics[2].name).toBe('Zits');
  });

  it('returns previous and next comics', () => {
    mockComicsAndPrefs([
      { id: 1, name: 'Archie' },
      { id: 2, name: 'Garfield' },
      { id: 3, name: 'Zits' },
    ]);

    const { result } = renderHook(() => useReadingList(2));

    expect(result.current.previousComic?.name).toBe('Archie');
    expect(result.current.nextComic?.name).toBe('Zits');
  });

  it('returns null for previous when at start', () => {
    mockComicsAndPrefs([
      { id: 1, name: 'Archie' },
      { id: 2, name: 'Garfield' },
    ]);

    const { result } = renderHook(() => useReadingList(1));

    expect(result.current.previousComic).toBeNull();
    expect(result.current.nextComic?.name).toBe('Garfield');
  });

  it('returns null for next when at end', () => {
    mockComicsAndPrefs([
      { id: 1, name: 'Archie' },
      { id: 2, name: 'Garfield' },
    ]);

    const { result } = renderHook(() => useReadingList(2));

    expect(result.current.previousComic?.name).toBe('Archie');
    expect(result.current.nextComic).toBeNull();
  });

  it('marks comics with unread when lastRead < newest', () => {
    mockComicsAndPrefs(
      [{ id: 1, name: 'Garfield', newest: '2026-03-20' }],
      [{ comicId: 1, date: '2026-03-15' }],
    );

    const { result } = renderHook(() => useReadingList(1));

    expect(result.current.comics[0].hasUnread).toBe(true);
  });

  it('marks comics as read when lastRead matches newest', () => {
    mockComicsAndPrefs(
      [{ id: 1, name: 'Garfield', newest: '2026-03-20' }],
      [{ comicId: 1, date: '2026-03-20' }],
    );

    const { result } = renderHook(() => useReadingList(1));

    expect(result.current.comics[0].hasUnread).toBe(false);
  });

  it('navigateToComic pushes correct URL', () => {
    mockComicsAndPrefs(
      [{ id: 1, name: 'Garfield' }],
      [{ comicId: 1, date: '2026-03-15' }],
    );

    const { result } = renderHook(() => useReadingList(1));

    result.current.navigateToComic(1);
    expect(mockPush).toHaveBeenCalledWith('/comics/1/read?date=2026-03-15');
  });

  it('navigateToComic omits date when no lastRead', () => {
    mockComicsAndPrefs([{ id: 1, name: 'Garfield' }]);

    const { result } = renderHook(() => useReadingList(1));

    result.current.navigateToComic(1);
    expect(mockPush).toHaveBeenCalledWith('/comics/1/read');
  });

  it('returns loading true when queries are loading', () => {
    vi.mocked(useGetComicsQuery).mockReturnValue({ data: null, isLoading: true } as any);
    vi.mocked(useGetUserPreferencesQuery).mockReturnValue({ data: null, isLoading: true } as any);

    const { result } = renderHook(() => useReadingList(1));

    expect(result.current.isLoading).toBe(true);
  });

  it('returns empty list when no data', () => {
    vi.mocked(useGetComicsQuery).mockReturnValue({ data: null, isLoading: false } as any);
    vi.mocked(useGetUserPreferencesQuery).mockReturnValue({ data: null, isLoading: false } as any);

    const { result } = renderHook(() => useReadingList(1));

    expect(result.current.comics).toHaveLength(0);
  });

  it('filters to favorites when navMode is favorites', () => {
    usePreferencesStore.setState({
      settings: { readerNavMode: 'favorites' } as any,
      isHydrated: true,
    });

    mockComicsAndPrefs(
      [
        { id: 1, name: 'Archie' },
        { id: 2, name: 'Garfield' },
        { id: 3, name: 'Zits' },
      ],
      [],
      [2],
    );

    const { result } = renderHook(() => useReadingList(2));

    expect(result.current.comics).toHaveLength(1);
    expect(result.current.comics[0].name).toBe('Garfield');
  });
});
