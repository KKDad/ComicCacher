import { renderHook, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import React from 'react';
import { toast } from 'sonner';
import { useRandomStrip } from './use-random-strip';

const backend = vi.hoisted(() => ({
  dates: [] as Array<string | null>,
  calls: [] as Array<Record<string, unknown>>,
  fail: false,
}));

vi.mock('@/lib/graphql-client', () => ({
  fetcher: (_query: unknown, variables: Record<string, unknown> = {}) => async () => {
    backend.calls.push(variables);
    if (backend.fail) throw new Error('boom');
    const date = backend.dates.shift() ?? null;
    return { randomStrip: date ? { date } : null };
  },
}));

vi.mock('sonner', () => ({ toast: { error: vi.fn() } }));

function renderRandomStrip(goToDate: (date: string) => void) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const wrapper = ({ children }: { children: React.ReactNode }) =>
    React.createElement(QueryClientProvider, { client: qc }, children);
  return renderHook(() => useRandomStrip(goToDate), { wrapper });
}

describe('useRandomStrip', () => {
  beforeEach(() => {
    backend.dates = [];
    backend.calls = [];
    backend.fail = false;
    vi.mocked(toast.error).mockClear();
  });

  it('goes to the random strip of the given comic', async () => {
    backend.dates = ['2026-02-01'];
    const goToDate = vi.fn();
    const { result } = renderRandomStrip(goToDate);

    await act(() => result.current.goToRandom(7));

    expect(backend.calls).toEqual([{ comicId: 7 }]);
    expect(goToDate).toHaveBeenCalledWith('2026-02-01');
    expect(result.current.isLoadingRandom).toBe(false);
  });

  it('fetches a new strip on every call', async () => {
    backend.dates = ['2026-02-01', '2026-03-15'];
    const goToDate = vi.fn();
    const { result } = renderRandomStrip(goToDate);

    await act(() => result.current.goToRandom(7));
    await act(() => result.current.goToRandom(7));

    expect(goToDate).toHaveBeenNthCalledWith(1, '2026-02-01');
    expect(goToDate).toHaveBeenNthCalledWith(2, '2026-03-15');
  });

  it('stays put when the comic has no strips', async () => {
    const goToDate = vi.fn();
    const { result } = renderRandomStrip(goToDate);

    await act(() => result.current.goToRandom(7));

    expect(goToDate).not.toHaveBeenCalled();
    expect(toast.error).not.toHaveBeenCalled();
  });

  it('shows a toast when the request fails', async () => {
    backend.fail = true;
    const goToDate = vi.fn();
    const { result } = renderRandomStrip(goToDate);

    await act(() => result.current.goToRandom(7));

    expect(goToDate).not.toHaveBeenCalled();
    expect(toast.error).toHaveBeenCalledWith('Could not load a random strip');
    expect(result.current.isLoadingRandom).toBe(false);
  });
});
