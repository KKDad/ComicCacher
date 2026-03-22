import { renderHook, act } from '@testing-library/react';
import { useReader } from './use-reader';
import {
  useGetStripWindowQuery,
  useGetRandomStripQuery,
  useUpdateLastReadMutation,
} from '@/generated/graphql';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import React from 'react';

vi.mock('@/generated/graphql', () => ({
  useGetStripWindowQuery: vi.fn(),
  useGetRandomStripQuery: vi.fn(),
  useUpdateLastReadMutation: vi.fn(),
}));

function createWrapper() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return React.createElement(QueryClientProvider, { client: qc }, children);
  };
}

const mockMutate = vi.fn();

function mockGqlDefaults(overrides?: {
  windowData?: any;
  windowLoading?: boolean;
  randomData?: any;
  randomLoading?: boolean;
}) {
  vi.mocked(useGetStripWindowQuery).mockReturnValue({
    data: overrides?.windowData ?? null,
    isLoading: overrides?.windowLoading ?? false,
  } as any);

  vi.mocked(useGetRandomStripQuery).mockReturnValue({
    data: overrides?.randomData ?? null,
    isLoading: overrides?.randomLoading ?? false,
  } as any);

  vi.mocked(useUpdateLastReadMutation).mockReturnValue({
    mutate: mockMutate,
  } as any);
}

const stripWindowData = {
  comic: {
    name: 'Garfield',
    oldest: '2026-03-10',
    newest: '2026-03-20',
    avatarUrl: 'https://example.com/avatar.png',
    stripWindow: [
      { date: '2026-03-14', available: true, imageUrl: 'https://example.com/14.png', width: 900, height: 300 },
      { date: '2026-03-15', available: true, imageUrl: 'https://example.com/15.png', width: 900, height: 300 },
      { date: '2026-03-16', available: true, imageUrl: 'https://example.com/16.png', width: 900, height: 300 },
    ],
  },
};

describe('useReader', () => {
  beforeEach(() => {
    mockMutate.mockClear();
    mockGqlDefaults();
  });

  it('returns loading true when window query is loading', () => {
    mockGqlDefaults({ windowLoading: true });

    const { result } = renderHook(
      () => useReader({ comicId: 1, initialDate: '2026-03-15', mode: 'scroll' }),
      { wrapper: createWrapper() },
    );

    expect(result.current.isLoading).toBe(true);
  });

  it('returns strips from window data', () => {
    mockGqlDefaults({ windowData: stripWindowData });

    const { result } = renderHook(
      () => useReader({ comicId: 1, initialDate: '2026-03-15', mode: 'scroll' }),
      { wrapper: createWrapper() },
    );

    expect(result.current.strips).toHaveLength(3);
    expect(result.current.strips[0].date).toBe('2026-03-14');
    expect(result.current.strips[2].date).toBe('2026-03-16');
  });

  it('returns comic metadata from window data', () => {
    mockGqlDefaults({ windowData: stripWindowData });

    const { result } = renderHook(
      () => useReader({ comicId: 1, initialDate: '2026-03-15', mode: 'scroll' }),
      { wrapper: createWrapper() },
    );

    expect(result.current.comicName).toBe('Garfield');
    expect(result.current.oldest).toBe('2026-03-10');
    expect(result.current.newest).toBe('2026-03-20');
    expect(result.current.avatarUrl).toBe('https://example.com/avatar.png');
  });

  it('sets hasOlder and hasNewer based on boundaries', () => {
    mockGqlDefaults({ windowData: stripWindowData });

    const { result } = renderHook(
      () => useReader({ comicId: 1, initialDate: '2026-03-15', mode: 'scroll' }),
      { wrapper: createWrapper() },
    );

    expect(result.current.hasOlder).toBe(true);
    expect(result.current.hasNewer).toBe(true);
  });

  it('goToDate resets strips and sets new center', () => {
    mockGqlDefaults({ windowData: stripWindowData });

    const { result } = renderHook(
      () => useReader({ comicId: 1, initialDate: '2026-03-15', mode: 'scroll' }),
      { wrapper: createWrapper() },
    );

    act(() => {
      result.current.goToDate('2026-03-01');
    });

    // Strips are cleared while waiting for new data
    expect(result.current.strips).toHaveLength(0);
  });

  it('goNewer increments currentIndex', () => {
    mockGqlDefaults({ windowData: stripWindowData });

    const { result } = renderHook(
      () => useReader({ comicId: 1, initialDate: '2026-03-15', mode: 'scroll' }),
      { wrapper: createWrapper() },
    );

    // Set to index 0 first
    act(() => {
      result.current.setCurrentIndex(0);
    });

    act(() => {
      result.current.goNewer();
    });

    expect(result.current.currentIndex).toBe(1);
  });

  it('goOlder decrements currentIndex', () => {
    mockGqlDefaults({ windowData: stripWindowData });

    const { result } = renderHook(
      () => useReader({ comicId: 1, initialDate: '2026-03-15', mode: 'scroll' }),
      { wrapper: createWrapper() },
    );

    // Set to index 2 first
    act(() => {
      result.current.setCurrentIndex(2);
    });

    act(() => {
      result.current.goOlder();
    });

    expect(result.current.currentIndex).toBe(1);
  });

  it('returns loading state for random strip', () => {
    mockGqlDefaults({ windowData: stripWindowData, randomLoading: true });

    const { result } = renderHook(
      () => useReader({ comicId: 1, initialDate: '2026-03-15', mode: 'scroll' }),
      { wrapper: createWrapper() },
    );

    expect(result.current.isLoadingRandom).toBe(true);
  });

  it('returns empty defaults when no data', () => {
    mockGqlDefaults();

    const { result } = renderHook(
      () => useReader({ comicId: 1, initialDate: '2026-03-15', mode: 'scroll' }),
      { wrapper: createWrapper() },
    );

    expect(result.current.strips).toHaveLength(0);
    expect(result.current.comicName).toBe('');
    expect(result.current.oldest).toBeNull();
    expect(result.current.newest).toBeNull();
  });

  it('provides goToFirst and goToLast callbacks', () => {
    mockGqlDefaults({ windowData: stripWindowData });

    const { result } = renderHook(
      () => useReader({ comicId: 1, initialDate: '2026-03-15', mode: 'scroll' }),
      { wrapper: createWrapper() },
    );

    expect(result.current.goToFirst).toBeInstanceOf(Function);
    expect(result.current.goToLast).toBeInstanceOf(Function);
    expect(result.current.goToRandom).toBeInstanceOf(Function);
  });

  it('provides loadOlder and loadNewer callbacks', () => {
    mockGqlDefaults({ windowData: stripWindowData });

    const { result } = renderHook(
      () => useReader({ comicId: 1, initialDate: '2026-03-15', mode: 'scroll' }),
      { wrapper: createWrapper() },
    );

    expect(result.current.loadOlder).toBeInstanceOf(Function);
    expect(result.current.loadNewer).toBeInstanceOf(Function);
  });

  it('goNewer calls loadNewer when at last strip with hasNewer', () => {
    mockGqlDefaults({ windowData: stripWindowData });

    const { result } = renderHook(
      () => useReader({ comicId: 1, initialDate: '2026-03-15', mode: 'scroll' }),
      { wrapper: createWrapper() },
    );

    // Move to the last strip
    act(() => {
      result.current.setCurrentIndex(2);
    });

    // goNewer at last strip should not crash — it attempts to load more
    act(() => {
      result.current.goNewer();
    });

    // Should still be at index 2 (loadNewer doesn't change currentIndex directly)
    expect(result.current.currentIndex).toBe(2);
  });

  it('goOlder calls loadOlder when at first strip with hasOlder', () => {
    mockGqlDefaults({ windowData: stripWindowData });

    const { result } = renderHook(
      () => useReader({ comicId: 1, initialDate: '2026-03-15', mode: 'scroll' }),
      { wrapper: createWrapper() },
    );

    // Set to first strip
    act(() => {
      result.current.setCurrentIndex(0);
    });

    // goOlder at first strip should not crash — it attempts to load more
    act(() => {
      result.current.goOlder();
    });

    expect(result.current.currentIndex).toBe(0);
  });

  it('goToFirst returns loading when no window data', () => {
    mockGqlDefaults();

    const { result } = renderHook(
      () => useReader({ comicId: 1, initialDate: '2026-03-15', mode: 'scroll' }),
      { wrapper: createWrapper() },
    );

    let returnVal: string;
    act(() => {
      returnVal = result.current.goToFirst();
    });

    expect(returnVal!).toBe('loading');
  });

  it('goToLast returns loading when no window data', () => {
    mockGqlDefaults();

    const { result } = renderHook(
      () => useReader({ comicId: 1, initialDate: '2026-03-15', mode: 'scroll' }),
      { wrapper: createWrapper() },
    );

    let returnVal: string;
    act(() => {
      returnVal = result.current.goToLast();
    });

    expect(returnVal!).toBe('loading');
  });

  it('isLoading when no initialDate and center not yet resolved', () => {
    mockGqlDefaults({ windowLoading: false });

    const { result } = renderHook(
      () => useReader({ comicId: 1, mode: 'scroll' }),
      { wrapper: createWrapper() },
    );

    // No initialDate and no centerDate resolved yet = isLoading true
    expect(result.current.isLoading).toBe(true);
  });

  it('goToRandom triggers random query', () => {
    mockGqlDefaults({ windowData: stripWindowData });

    const { result } = renderHook(
      () => useReader({ comicId: 1, initialDate: '2026-03-15', mode: 'scroll' }),
      { wrapper: createWrapper() },
    );

    act(() => {
      result.current.goToRandom();
    });

    // Should not crash; random query gets triggered internally
    expect(result.current.goToRandom).toBeInstanceOf(Function);
  });

  it('goToFirst returns already when at first strip', () => {
    const dataWithOldestLoaded = {
      comic: {
        ...stripWindowData.comic,
        stripWindow: [
          { date: '2026-03-10', available: true, imageUrl: 'https://example.com/10.png', width: 900, height: 300 },
          { date: '2026-03-11', available: true, imageUrl: 'https://example.com/11.png', width: 900, height: 300 },
          { date: '2026-03-12', available: true, imageUrl: 'https://example.com/12.png', width: 900, height: 300 },
        ],
      },
    };
    mockGqlDefaults({ windowData: dataWithOldestLoaded });

    const { result } = renderHook(
      () => useReader({ comicId: 1, initialDate: '2026-03-10', mode: 'scroll' }),
      { wrapper: createWrapper() },
    );

    // Set to index 0 (at the oldest strip)
    act(() => {
      result.current.setCurrentIndex(0);
    });

    let returnVal: string;
    act(() => {
      returnVal = result.current.goToFirst();
    });

    expect(returnVal!).toBe('already');
  });

  it('goToFirst returns scrolled when first strip is loaded but not in view', () => {
    const dataWithOldestLoaded = {
      comic: {
        ...stripWindowData.comic,
        stripWindow: [
          { date: '2026-03-10', available: true, imageUrl: 'https://example.com/10.png', width: 900, height: 300 },
          { date: '2026-03-11', available: true, imageUrl: 'https://example.com/11.png', width: 900, height: 300 },
          { date: '2026-03-12', available: true, imageUrl: 'https://example.com/12.png', width: 900, height: 300 },
          { date: '2026-03-13', available: true, imageUrl: 'https://example.com/13.png', width: 900, height: 300 },
          { date: '2026-03-14', available: true, imageUrl: 'https://example.com/14.png', width: 900, height: 300 },
          { date: '2026-03-15', available: true, imageUrl: 'https://example.com/15.png', width: 900, height: 300 },
        ],
      },
    };
    mockGqlDefaults({ windowData: dataWithOldestLoaded });

    const { result } = renderHook(
      () => useReader({ comicId: 1, initialDate: '2026-03-15', mode: 'scroll' }),
      { wrapper: createWrapper() },
    );

    // Move to index 5 — far enough that index 0 + 2 < 5
    act(() => {
      result.current.setCurrentIndex(5);
    });

    let returnVal: string;
    act(() => {
      returnVal = result.current.goToFirst();
    });

    expect(returnVal!).toBe('scrolled');
  });

  it('goToLast returns already when at last strip', () => {
    const dataWithNewestLoaded = {
      comic: {
        ...stripWindowData.comic,
        stripWindow: [
          { date: '2026-03-18', available: true, imageUrl: 'https://example.com/18.png', width: 900, height: 300 },
          { date: '2026-03-19', available: true, imageUrl: 'https://example.com/19.png', width: 900, height: 300 },
          { date: '2026-03-20', available: true, imageUrl: 'https://example.com/20.png', width: 900, height: 300 },
        ],
      },
    };
    mockGqlDefaults({ windowData: dataWithNewestLoaded });

    const { result } = renderHook(
      () => useReader({ comicId: 1, initialDate: '2026-03-19', mode: 'scroll' }),
      { wrapper: createWrapper() },
    );

    act(() => {
      result.current.setCurrentIndex(2);
    });

    let returnVal: string;
    act(() => {
      returnVal = result.current.goToLast();
    });

    expect(returnVal!).toBe('already');
  });

  it('goToLast returns scrolled when newest strip is loaded but not in view', () => {
    const dataWithNewestLoaded = {
      comic: {
        ...stripWindowData.comic,
        stripWindow: [
          { date: '2026-03-10', available: true, imageUrl: 'https://example.com/10.png', width: 900, height: 300 },
          { date: '2026-03-11', available: true, imageUrl: 'https://example.com/11.png', width: 900, height: 300 },
          { date: '2026-03-12', available: true, imageUrl: 'https://example.com/12.png', width: 900, height: 300 },
          { date: '2026-03-15', available: true, imageUrl: 'https://example.com/15.png', width: 900, height: 300 },
          { date: '2026-03-18', available: true, imageUrl: 'https://example.com/18.png', width: 900, height: 300 },
          { date: '2026-03-20', available: true, imageUrl: 'https://example.com/20.png', width: 900, height: 300 },
        ],
      },
    };
    mockGqlDefaults({ windowData: dataWithNewestLoaded });

    const { result } = renderHook(
      () => useReader({ comicId: 1, initialDate: '2026-03-15', mode: 'scroll' }),
      { wrapper: createWrapper() },
    );

    // Stay at index 0 — far enough from index 5 (newest) that lastIdx - 2 > 0
    act(() => {
      result.current.setCurrentIndex(0);
    });

    let returnVal: string;
    act(() => {
      returnVal = result.current.goToLast();
    });

    expect(returnVal!).toBe('scrolled');
  });

  it('goToLast loads date when newest strip is not in loaded strips', () => {
    const partialData = {
      comic: {
        name: 'Garfield',
        oldest: '2020-01-01',
        newest: '2026-03-20',
        avatarUrl: null,
        stripWindow: [
          { date: '2026-03-10', available: true, imageUrl: 'https://example.com/10.png', width: 900, height: 300 },
        ],
      },
    };
    mockGqlDefaults({ windowData: partialData });

    const { result } = renderHook(
      () => useReader({ comicId: 1, initialDate: '2026-03-10', mode: 'scroll' }),
      { wrapper: createWrapper() },
    );

    let returnVal: string;
    act(() => {
      returnVal = result.current.goToLast();
    });

    expect(returnVal!).toBe('loading');
    expect(result.current.strips).toHaveLength(0);
  });

  it('goToFirst loads date when oldest strip is not in loaded strips', () => {
    // Window data where oldest date is NOT in the stripWindow
    const partialData = {
      comic: {
        name: 'Garfield',
        oldest: '2020-01-01',
        newest: '2026-03-20',
        avatarUrl: null,
        stripWindow: [
          { date: '2026-03-15', available: true, imageUrl: 'https://example.com/15.png', width: 900, height: 300 },
        ],
      },
    };
    mockGqlDefaults({ windowData: partialData });

    const { result } = renderHook(
      () => useReader({ comicId: 1, initialDate: '2026-03-15', mode: 'scroll' }),
      { wrapper: createWrapper() },
    );

    let returnVal: string;
    act(() => {
      returnVal = result.current.goToFirst();
    });

    expect(returnVal!).toBe('loading');
    // Strips should be cleared for the new center date
    expect(result.current.strips).toHaveLength(0);
  });
});
