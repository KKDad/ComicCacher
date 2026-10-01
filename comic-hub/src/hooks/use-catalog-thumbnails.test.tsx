import { act, renderHook } from '@testing-library/react';
import { useCatalogThumbnails, entriesNeedingThumbnails, RETRY_MS } from './use-catalog-thumbnails';
import { mockEntry, mockSourceComic } from '@/test/source-fixtures';
import { mockMutationResult } from '@/test/mock-query';
import { useRequestCatalogThumbnailsMutation } from '@/generated/graphql';
import type { CatalogEntry } from '@/types/sources';

vi.mock('@/generated/graphql', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/generated/graphql')>()),
  useRequestCatalogThumbnailsMutation: vi.fn(),
}));

describe('entriesNeedingThumbnails', () => {
  it('skips entries that have a picture, are being fetched, or are gone', () => {
    const entries = [
      mockEntry({ identifier: 'wanted' }),
      mockEntry({ identifier: 'cached', thumbnailUrl: '/api/v1/sources/gocomics/thumbnails/cached' }),
      mockEntry({ identifier: 'pending', thumbnailPending: true }),
      mockEntry({ identifier: 'gone', removedAt: '2026-09-01T00:00:00Z' }),
      mockEntry({ identifier: 'avatar', comic: mockSourceComic() }),
      mockEntry({ identifier: 'no-avatar', comic: mockSourceComic({ avatarAvailable: false }) }),
    ];

    expect(entriesNeedingThumbnails(entries)).toEqual(['wanted', 'no-avatar']);
  });
});

describe('useCatalogThumbnails', () => {
  const mutate = vi.fn();

  beforeEach(() => {
    vi.useFakeTimers();
    mutate.mockReset();
    vi.mocked(useRequestCatalogThumbnailsMutation).mockReturnValue(mockMutationResult({ mutate }));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('asks once, a moment after entries appear, and says when work was queued', () => {
    const onQueued = vi.fn();
    const visible: CatalogEntry[] = [mockEntry({ identifier: 'a' }), mockEntry({ identifier: 'b' })];
    const { rerender } = renderHook(({ entries }) => useCatalogThumbnails('gocomics', entries, onQueued), { initialProps: { entries: visible } });

    expect(mutate).not.toHaveBeenCalled();
    act(() => vi.advanceTimersByTime(500));
    expect(mutate).toHaveBeenCalledWith({ source: 'gocomics', identifiers: ['a', 'b'] }, expect.anything());

    mutate.mock.calls[0][1].onSuccess({ requestCatalogThumbnails: { queued: 2 } });
    expect(onQueued).toHaveBeenCalled();

    // The same entries again (say, after a poll) aren't asked for twice
    rerender({ entries: [...visible] });
    act(() => vi.advanceTimersByTime(500));
    expect(mutate).toHaveBeenCalledTimes(1);

    // A new entry is
    rerender({ entries: [...visible, mockEntry({ identifier: 'c' })] });
    act(() => vi.advanceTimersByTime(500));
    expect(mutate).toHaveBeenLastCalledWith({ source: 'gocomics', identifiers: ['c'] }, expect.anything());
  });

  it('asks again a while after a failed request', () => {
    renderHook(() => useCatalogThumbnails('gocomics', [mockEntry({ identifier: 'a' })], vi.fn()));
    act(() => vi.advanceTimersByTime(500));
    act(() => mutate.mock.calls[0][1].onError(new Error('down')));

    act(() => vi.advanceTimersByTime(500));
    expect(mutate).toHaveBeenCalledTimes(1);
    act(() => vi.advanceTimersByTime(RETRY_MS));
    expect(mutate).toHaveBeenCalledTimes(2);
  });

  it('does nothing when every entry has a picture', () => {
    renderHook(() => useCatalogThumbnails('gocomics', [mockEntry({ thumbnailUrl: '/x' })], vi.fn()));
    act(() => vi.advanceTimersByTime(1000));
    expect(mutate).not.toHaveBeenCalled();
  });

  it('does not say anything was queued when nothing was', () => {
    const onQueued = vi.fn();
    renderHook(() => useCatalogThumbnails('gocomics', [mockEntry()], onQueued));
    act(() => vi.advanceTimersByTime(500));
    mutate.mock.calls[0][1].onSuccess({ requestCatalogThumbnails: { queued: 0 } });
    expect(onQueued).not.toHaveBeenCalled();
  });
});
