import { renderHook, act, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import React from 'react';
import { toast } from 'sonner';
import { useReader } from './use-reader';

// A fake backend behind the generated hooks' fetcher, so the real TanStack Query
// paging runs. stripWindow mirrors ComicManagementFacade.getStripWindow: up to
// `before` strips, the centre date, then up to `after` strips.
const backend = vi.hoisted(() => ({
  dates: [] as string[],
  calls: [] as Array<{ op: string; variables: Record<string, unknown> }>,
  randomDate: '2026-02-01' as string | null,
  failRandom: false,
  // When set, stripWindow requests wait until it resolves
  gate: null as Promise<void> | null,
}));

vi.mock('@/lib/graphql-client', () => ({
  fetcher: (query: { toString(): string }, variables: Record<string, unknown> = {}) => async () => {
    const doc = query.toString();
    if (doc.includes('stripWindow')) {
      backend.calls.push({ op: 'stripWindow', variables });
      if (backend.gate) await backend.gate;
      const { center, before, after } = variables as { center: string; before: number; after: number };
      const { dates } = backend;
      const idx = dates.indexOf(center);
      const window = idx >= 0
        ? dates.slice(Math.max(0, idx - before), idx + after + 1)
        : [dates.at(-1)!]; // far-future centre: the backend clamps to the newest
      return {
        comic: {
          id: 1,
          name: 'Freefall',
          oldest: dates[0],
          newest: dates.at(-1),
          avatarUrl: null,
          stripWindow: window.map((date) => ({
            date,
            available: true,
            imageUrl: `https://example.com/${date}.png`,
            width: 900,
            height: 300,
          })),
        },
      };
    }
    if (doc.includes('randomStrip')) {
      backend.calls.push({ op: 'randomStrip', variables });
      if (backend.failRandom) throw new Error('boom');
      return { randomStrip: backend.randomDate ? { date: backend.randomDate } : null };
    }
    if (doc.includes('updateLastRead')) {
      backend.calls.push({ op: 'updateLastRead', variables });
      return { updateLastRead: { preference: null, errors: [] } };
    }
    throw new Error(`Unexpected query: ${doc.slice(0, 40)}`);
  },
}));

vi.mock('sonner', () => ({ toast: { error: vi.fn() } }));

/** Every day from `from` for `days` days, as YYYY-MM-DD. */
function daily(from: string, days: number): string[] {
  const start = new Date(`${from}T12:00:00Z`);
  return Array.from({ length: days }, (_, i) => {
    const d = new Date(start);
    d.setUTCDate(start.getUTCDate() + i);
    return d.toISOString().slice(0, 10);
  });
}

function createWrapper() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return React.createElement(QueryClientProvider, { client: qc }, children);
  };
}

type Options = Parameters<typeof useReader>[0];

async function renderReader(options: Partial<Options> = {}) {
  const hook = renderHook(
    () => useReader({ comicId: 1, initialDate: '2026-02-15', mode: 'scroll', ...options }),
    { wrapper: createWrapper() },
  );
  await waitFor(() => expect(hook.result.current.strips.length).toBeGreaterThan(0));
  return hook;
}

const stripCalls = () => backend.calls.filter((c) => c.op === 'stripWindow');
const lastReadDates = () =>
  backend.calls.filter((c) => c.op === 'updateLastRead').map((c) => c.variables.date);
const currentDate = (r: { current: ReturnType<typeof useReader> }) =>
  r.current.strips[r.current.currentIndex]?.date;

describe('useReader', () => {
  beforeEach(() => {
    backend.dates = daily('2026-01-01', 90); // 2026-01-01 .. 2026-03-31
    backend.calls = [];
    backend.randomDate = '2026-02-01';
    backend.failRandom = false;
    backend.gate = null;
    vi.mocked(toast.error).mockClear();
  });

  describe('initial load', () => {
    it('loads 10 strips either side of the initial date in one request', async () => {
      const { result } = await renderReader();

      expect(result.current.strips).toHaveLength(21);
      expect(result.current.strips[0].date).toBe('2026-02-05');
      expect(result.current.strips.at(-1)!.date).toBe('2026-02-25');
      expect(currentDate(result)).toBe('2026-02-15');
      expect(stripCalls()).toHaveLength(1);
    });

    it('exposes the comic metadata', async () => {
      const { result } = await renderReader();

      expect(result.current.comicName).toBe('Freefall');
      expect(result.current.oldest).toBe('2026-01-01');
      expect(result.current.newest).toBe('2026-03-31');
      expect(result.current.avatarUrl).toBeNull();
      expect(result.current.isLoading).toBe(false);
    });

    it('starts at the newest strip when no date is given', async () => {
      const { result } = await renderReader({ initialDate: undefined });

      expect(currentDate(result)).toBe('2026-03-31');
      expect(result.current.hasNewer).toBe(false);
      expect(result.current.hasOlder).toBe(true);
    });

    it('is loading until the newest date is known', () => {
      backend.gate = new Promise(() => {});
      const { result } = renderHook(
        () => useReader({ comicId: 1, mode: 'scroll' }),
        { wrapper: createWrapper() },
      );

      expect(result.current.isLoading).toBe(true);
      expect(result.current.strips).toHaveLength(0);
    });

    it('reports no older or newer strips at the ends of the archive', async () => {
      backend.dates = daily('2026-01-01', 5);
      const { result } = await renderReader({ initialDate: '2026-01-03' });

      expect(result.current.strips).toHaveLength(5);
      expect(result.current.hasOlder).toBe(false);
      expect(result.current.hasNewer).toBe(false);
    });
  });

  describe('paging', () => {
    it('loadOlder prepends 20 strips without duplicating the shared edge strip', async () => {
      const { result } = await renderReader();

      act(() => result.current.loadOlder());
      await waitFor(() => expect(result.current.strips).toHaveLength(41));

      const dates = result.current.strips.map((s) => s.date);
      expect(new Set(dates).size).toBe(dates.length);
      expect(dates).toEqual([...dates].sort());
      expect(dates[0]).toBe('2026-01-16');
      expect(stripCalls().at(-1)!.variables).toMatchObject({ center: '2026-02-05', before: 20, after: 0 });
    });

    it('loadNewer appends 20 strips', async () => {
      const { result } = await renderReader();

      act(() => result.current.loadNewer());
      await waitFor(() => expect(result.current.strips).toHaveLength(41));

      expect(result.current.strips.at(-1)!.date).toBe('2026-03-17');
      expect(result.current.hasNewer).toBe(true);
      expect(stripCalls().at(-1)!.variables).toMatchObject({ center: '2026-02-25', before: 0, after: 20 });
    });

    it('keeps the current strip when older strips are prepended', async () => {
      const { result } = await renderReader();
      const indexBefore = result.current.currentIndex;

      act(() => result.current.loadOlder());
      await waitFor(() => expect(result.current.strips).toHaveLength(41));

      expect(currentDate(result)).toBe('2026-02-15');
      expect(result.current.currentIndex).toBe(indexBefore + 20);
    });

    it('does not start a second request while a page is loading', async () => {
      const { result } = await renderReader();
      let release!: () => void;
      backend.gate = new Promise((resolve) => { release = resolve; });

      act(() => result.current.loadOlder());
      await waitFor(() => expect(result.current.isFetchingOlder).toBe(true));
      act(() => {
        result.current.loadOlder();
        result.current.loadNewer();
      });

      expect(stripCalls()).toHaveLength(2);
      await act(async () => release());
      await waitFor(() => expect(result.current.isFetchingOlder).toBe(false));
      expect(stripCalls()).toHaveLength(2);
    });

    it('does nothing at the ends of the archive', async () => {
      backend.dates = daily('2026-01-01', 5);
      const { result } = await renderReader({ initialDate: '2026-01-03' });

      act(() => {
        result.current.loadOlder();
        result.current.loadNewer();
      });

      expect(stripCalls()).toHaveLength(1);
    });
  });

  describe('step navigation', () => {
    it('goNewer and goOlder move one strip within the loaded list', async () => {
      const { result } = await renderReader();

      act(() => result.current.goNewer());
      expect(currentDate(result)).toBe('2026-02-16');

      act(() => result.current.goOlder());
      act(() => result.current.goOlder());
      expect(currentDate(result)).toBe('2026-02-14');
    });

    it('goOlder at the first loaded strip loads a page and steps back one strip', async () => {
      const { result } = await renderReader();
      act(() => result.current.setCurrentIndex(0));
      expect(currentDate(result)).toBe('2026-02-05');

      act(() => result.current.goOlder());

      await waitFor(() => expect(currentDate(result)).toBe('2026-02-04'));
      expect(result.current.strips).toHaveLength(41);
    });

    it('goNewer at the last loaded strip loads a page and steps forward one strip', async () => {
      const { result } = await renderReader();
      act(() => result.current.setCurrentIndex(20));
      expect(currentDate(result)).toBe('2026-02-25');

      act(() => result.current.goNewer());

      await waitFor(() => expect(currentDate(result)).toBe('2026-02-26'));
    });

    it('goOlder and goNewer stay put at the ends of the archive', async () => {
      backend.dates = daily('2026-01-01', 3);
      const { result } = await renderReader({ initialDate: '2026-01-01' });

      act(() => result.current.goOlder());
      expect(currentDate(result)).toBe('2026-01-01');

      act(() => result.current.setCurrentIndex(2));
      act(() => result.current.goNewer());
      expect(currentDate(result)).toBe('2026-01-03');
      expect(stripCalls()).toHaveLength(1);
    });

    it('setCurrentIndex ignores an index outside the list', async () => {
      const { result } = await renderReader();

      act(() => result.current.setCurrentIndex(99));

      expect(currentDate(result)).toBe('2026-02-15');
    });
  });

  describe('jumping', () => {
    it('goToDate builds a new list around the date', async () => {
      const { result } = await renderReader();

      act(() => result.current.goToDate('2026-01-20'));
      await waitFor(() => expect(currentDate(result)).toBe('2026-01-20'));

      expect(result.current.strips[0].date).toBe('2026-01-10');
      expect(result.current.strips).toHaveLength(21);
    });

    it('goToFirst reports already when the first strip is in view', async () => {
      const { result } = await renderReader({ initialDate: '2026-01-02' });

      let outcome: string | undefined;
      act(() => { outcome = result.current.goToFirst(); });

      expect(outcome).toBe('already');
    });

    it('goToFirst scrolls to a loaded first strip', async () => {
      const { result } = await renderReader({ initialDate: '2026-01-08' });

      let outcome: string | undefined;
      act(() => { outcome = result.current.goToFirst(); });

      expect(outcome).toBe('scrolled');
      expect(currentDate(result)).toBe('2026-01-01');
    });

    it('goToFirst loads the first strip when it is not loaded', async () => {
      const { result } = await renderReader();

      let outcome: string | undefined;
      act(() => { outcome = result.current.goToFirst(); });

      expect(outcome).toBe('loading');
      await waitFor(() => expect(currentDate(result)).toBe('2026-01-01'));
    });

    it('goToLast reports already, scrolls, or loads', async () => {
      const { result } = await renderReader({ initialDate: '2026-03-30' });
      let outcome: string | undefined;
      act(() => { outcome = result.current.goToLast(); });
      expect(outcome).toBe('already');

      act(() => result.current.setCurrentIndex(0));
      act(() => { outcome = result.current.goToLast(); });
      expect(outcome).toBe('scrolled');
      expect(currentDate(result)).toBe('2026-03-31');

      act(() => result.current.goToDate('2026-01-15'));
      await waitFor(() => expect(currentDate(result)).toBe('2026-01-15'));
      act(() => { outcome = result.current.goToLast(); });
      expect(outcome).toBe('loading');
      await waitFor(() => expect(currentDate(result)).toBe('2026-03-31'));
    });

    it('goToFirst and goToLast report loading before the comic is known', () => {
      backend.gate = new Promise(() => {});
      const { result } = renderHook(
        () => useReader({ comicId: 1, initialDate: '2026-02-15', mode: 'scroll' }),
        { wrapper: createWrapper() },
      );

      expect(result.current.goToFirst()).toBe('loading');
      expect(result.current.goToLast()).toBe('loading');
    });

    it('goToRandom jumps to the random strip', async () => {
      const { result } = await renderReader();

      await act(async () => result.current.goToRandom());

      await waitFor(() => expect(currentDate(result)).toBe('2026-02-01'));
      expect(result.current.isLoadingRandom).toBe(false);
    });

    it('goToRandom stays put when there is no random strip', async () => {
      backend.randomDate = null;
      const { result } = await renderReader();

      await act(async () => result.current.goToRandom());

      expect(currentDate(result)).toBe('2026-02-15');
    });

    it('goToRandom shows an error toast when the request fails', async () => {
      backend.failRandom = true;
      const { result } = await renderReader();

      await act(async () => result.current.goToRandom());

      expect(toast.error).toHaveBeenCalledWith('Could not load a random strip');
      expect(result.current.isLoadingRandom).toBe(false);
    });
  });

  describe('last-read tracking', () => {
    it('snap mode saves the current strip immediately', async () => {
      await renderReader({ mode: 'snap' });

      await waitFor(() => expect(lastReadDates()).toEqual(['2026-02-15']));
    });

    it('scroll mode saves the current strip after a 1s pause', async () => {
      await renderReader();

      expect(lastReadDates()).toEqual([]);
      await waitFor(() => expect(lastReadDates()).toEqual(['2026-02-15']), { timeout: 2000 });
    });

    it('does not save a different strip when older strips are prepended', async () => {
      const { result } = await renderReader({ mode: 'snap' });
      await waitFor(() => expect(lastReadDates()).toEqual(['2026-02-15']));

      act(() => result.current.loadOlder());
      await waitFor(() => expect(result.current.strips).toHaveLength(41));

      expect(lastReadDates()).toEqual(['2026-02-15']);
    });
  });
});
