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
});
