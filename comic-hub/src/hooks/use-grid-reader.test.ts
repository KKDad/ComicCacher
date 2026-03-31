import { renderHook } from '@testing-library/react';
import { useGridReader } from './use-grid-reader';
import { useGetComicsForDateQuery, useGetUserPreferencesQuery } from '@/generated/graphql';
import { usePreferencesStore } from '@/stores/preferences-store';
import { useQueryClient } from '@tanstack/react-query';

vi.mock('@/generated/graphql', () => ({
  useGetComicsForDateQuery: Object.assign(vi.fn(), {
    getKey: vi.fn().mockReturnValue(['GetComicsForDate', {}]),
    fetcher: vi.fn().mockReturnValue(() => Promise.resolve({})),
  }),
  useGetUserPreferencesQuery: vi.fn(),
}));

vi.mock('@tanstack/react-query', () => ({
  useQueryClient: vi.fn(),
}));

vi.mock('@/stores/preferences-store', () => ({
  usePreferencesStore: vi.fn(),
}));

const mockReplace = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({ replace: mockReplace }),
  useSearchParams: () => new URLSearchParams('date=2026-03-29'),
}));

const mockComicsData = {
  comics: {
    edges: [
      {
        node: {
          id: 1, name: 'Garfield', avatarUrl: '/avatar/1', oldest: '2020-01-01', newest: '2026-03-29',
          strip: { date: '2026-03-29', available: true, imageUrl: '/strip/1/2026-03-29', width: 900, height: 300, transcript: null },
        },
      },
      {
        node: {
          id: 2, name: 'Dilbert', avatarUrl: '/avatar/2', oldest: '2019-01-01', newest: '2026-03-29',
          strip: { date: '2026-03-29', available: true, imageUrl: '/strip/2/2026-03-29', width: 800, height: 400, transcript: 'Hello' },
        },
      },
    ],
    totalCount: 2,
  },
};

const mockPrefsData = {
  preferences: {
    username: 'test',
    favoriteComics: [1],
    lastReadDates: [],
    displaySettings: null,
  },
};

describe('useGridReader', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useGetComicsForDateQuery).mockReturnValue({ data: mockComicsData, isLoading: false } as ReturnType<typeof useGetComicsForDateQuery>);
    vi.mocked(useGetUserPreferencesQuery).mockReturnValue({ data: mockPrefsData, isLoading: false } as ReturnType<typeof useGetUserPreferencesQuery>);
    vi.mocked(usePreferencesStore).mockImplementation((selector: (s: { settings: { readerNavMode: string } }) => string) =>
      selector({ settings: { readerNavMode: 'all' } }),
    );
    vi.mocked(useQueryClient).mockReturnValue({ prefetchQuery: vi.fn() } as unknown as ReturnType<typeof useQueryClient>);
  });

  it('reads date from search params', () => {
    const { result } = renderHook(() => useGridReader());
    expect(result.current.date).toBe('2026-03-29');
  });

  it('returns all comics sorted by name when navMode is all', () => {
    const { result } = renderHook(() => useGridReader());
    expect(result.current.comics).toHaveLength(2);
    expect(result.current.comics[0].name).toBe('Dilbert');
    expect(result.current.comics[1].name).toBe('Garfield');
  });

  it('filters to favorites when navMode is favorites', () => {
    vi.mocked(usePreferencesStore).mockImplementation((selector: (s: { settings: { readerNavMode: string } }) => string) =>
      selector({ settings: { readerNavMode: 'favorites' } }),
    );
    const { result } = renderHook(() => useGridReader());
    expect(result.current.comics).toHaveLength(1);
    expect(result.current.comics[0].name).toBe('Garfield');
  });

  it('returns empty comics when data is loading', () => {
    vi.mocked(useGetComicsForDateQuery).mockReturnValue({ data: undefined, isLoading: true } as ReturnType<typeof useGetComicsForDateQuery>);
    const { result } = renderHook(() => useGridReader());
    expect(result.current.comics).toHaveLength(0);
    expect(result.current.isLoading).toBe(true);
  });

  it('exposes navigation functions', () => {
    const { result } = renderHook(() => useGridReader());
    expect(result.current.goToDate).toBeInstanceOf(Function);
    expect(result.current.goToNextDate).toBeInstanceOf(Function);
    expect(result.current.goToPreviousDate).toBeInstanceOf(Function);
    expect(result.current.goToToday).toBeInstanceOf(Function);
  });

  it('maps strip data correctly', () => {
    const { result } = renderHook(() => useGridReader());
    const garfield = result.current.comics.find((c) => c.name === 'Garfield');
    expect(garfield?.strip?.available).toBe(true);
    expect(garfield?.strip?.imageUrl).toBe('/strip/1/2026-03-29');
    expect(garfield?.strip?.width).toBe(900);
  });

  it('handles comics with null strip', () => {
    const dataWithNullStrip = {
      comics: {
        edges: [
          { node: { id: 1, name: 'Test', avatarUrl: null, oldest: null, newest: null, strip: null } },
        ],
        totalCount: 1,
      },
    };
    vi.mocked(useGetComicsForDateQuery).mockReturnValue({ data: dataWithNullStrip, isLoading: false } as ReturnType<typeof useGetComicsForDateQuery>);
    const { result } = renderHook(() => useGridReader());
    expect(result.current.comics[0].strip).toBeNull();
  });
});
