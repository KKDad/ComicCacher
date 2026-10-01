import { renderHook } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { toast } from 'sonner';
import { useSourceActions } from './use-source-actions';
import { captureMutation } from '@/test/mock-query';
import { mockSourceComic } from '@/test/source-fixtures';
import {
  useAddComicFromCatalogMutation,
  useDetectComicStartMutation,
  useFetchComicAvatarMutation,
  useRefreshSourceCatalogMutation,
  useTriggerJobMutation,
  useUpdateSourceComicMutation,
} from '@/generated/graphql';

vi.mock('sonner', () => ({ toast: { success: vi.fn(), error: vi.fn() } }));

vi.mock('@/generated/graphql', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/generated/graphql')>()),
  useAddComicFromCatalogMutation: vi.fn(),
  useDetectComicStartMutation: vi.fn(),
  useFetchComicAvatarMutation: vi.fn(),
  useRefreshSourceCatalogMutation: vi.fn(),
  useTriggerJobMutation: vi.fn(),
  useUpdateSourceComicMutation: vi.fn(),
}));

function setup() {
  const mutate = vi.fn();
  const result = { mutate, isPending: false };
  const add = captureMutation(useAddComicFromCatalogMutation, result);
  const update = captureMutation(useUpdateSourceComicMutation, result);
  const refresh = captureMutation(useRefreshSourceCatalogMutation, result);
  const backfill = captureMutation(useTriggerJobMutation, result);
  const avatar = captureMutation(useFetchComicAvatarMutation, result);
  const detect = captureMutation(useDetectComicStartMutation, result);
  const queryClient = new QueryClient();
  const invalidate = vi.spyOn(queryClient, 'invalidateQueries');
  const { result: hook } = renderHook(() => useSourceActions(), {
    wrapper: ({ children }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>,
  });
  return { hook, mutate, invalidate, add, update, refresh, backfill, avatar, detect };
}

describe('useSourceActions', () => {
  beforeEach(() => vi.clearAllMocks());

  it('sends each action with the right variables', () => {
    const { hook, mutate } = setup();

    hook.current.addComic('gocomics', 'peanuts', true, false);
    hook.current.updateComic(3, { active: false });
    hook.current.refreshCatalog('gocomics');
    hook.current.backfillSource('gocomics');
    hook.current.backfillComic(3);
    hook.current.fetchAvatar(3);
    hook.current.detectStart(3);

    expect(mutate.mock.calls.map((call) => call[0])).toEqual([
      { input: { source: 'gocomics', identifier: 'peanuts', active: true, enabled: false } },
      { id: 3, input: { active: false } },
      { source: 'gocomics' },
      { jobName: 'ComicBackfillJob', parameters: { source: 'gocomics' } },
      { jobName: 'ComicBackfillJob', parameters: { comic: '3' } },
      { id: 3 },
      { id: 3 },
    ]);
  });

  it('announces success and refreshes the affected queries', () => {
    const { add, invalidate } = setup();

    add.succeed({ addComicFromCatalog: { comic: mockSourceComic({ name: 'Peanuts' }), errors: [] } }, { input: { source: 'gocomics', identifier: 'peanuts' } });

    expect(toast.success).toHaveBeenCalledWith('Peanuts added');
    expect(invalidate).toHaveBeenCalledTimes(1);
    const predicate = invalidate.mock.calls[0][0]?.predicate as unknown as (query: { queryKey: unknown[] }) => boolean;
    expect(predicate({ queryKey: ['GetSourceCatalog.infinite', {}] })).toBe(true);
    expect(predicate({ queryKey: ['GetSources'] })).toBe(true);
    expect(predicate({ queryKey: ['GetComics.infinite'] })).toBe(true);
    expect(predicate({ queryKey: ['GetBatchSchedulers'] })).toBe(false);
  });

  it('reports payload errors without refreshing', () => {
    const { add, update, refresh, backfill, avatar, detect, invalidate } = setup();

    add.succeed({ addComicFromCatalog: { comic: null, errors: [{ message: 'Already added', field: 'input.identifier', code: null }] } }, { input: { source: 's', identifier: 'i' } });
    update.succeed({ updateComic: { comic: null, errors: [{ message: 'Bad date', field: 'input.sourceStartDate', code: null }] } }, { id: 1, input: {} });
    refresh.succeed({ refreshSourceCatalog: { batchJob: null, errors: [{ message: 'Already refreshing', field: 'source' }] } }, { source: 's' });
    backfill.succeed({ triggerJob: { batchJob: null, errors: [{ message: 'Job not available', field: 'jobName', code: null }] } }, { jobName: 'ComicBackfillJob' });
    avatar.succeed({ fetchComicAvatar: { queued: false, errors: [{ message: 'Queue full' }] } }, { id: 1 });
    detect.succeed({ detectComicStart: { queued: false, errors: [] } }, { id: 1 });

    expect(vi.mocked(toast.error).mock.calls.map((call) => call[0])).toEqual([
      'Already added',
      'Bad date',
      'Already refreshing',
      'Job not available',
      'Queue full',
      'Start detection could not be queued',
    ]);
    expect(invalidate).not.toHaveBeenCalled();
  });

  it('announces queued work and started jobs', () => {
    const { update, refresh, backfill, avatar, detect } = setup();

    update.succeed({ updateComic: { comic: mockSourceComic(), errors: [] } }, { id: 1, input: { active: true } });
    refresh.succeed({ refreshSourceCatalog: { batchJob: { executionId: 1, status: 'STARTED' }, errors: [] } }, { source: 's' });
    backfill.succeed({ triggerJob: { batchJob: { executionId: 2, jobName: 'ComicBackfillJob', status: 'STARTED', startTime: '2026-09-28T11:00:00Z' }, errors: [] } }, { jobName: 'ComicBackfillJob' });
    avatar.succeed({ fetchComicAvatar: { queued: true, errors: [] } }, { id: 1 });
    detect.succeed({ detectComicStart: { queued: true, errors: [] } }, { id: 1 });

    expect(vi.mocked(toast.success).mock.calls.map((call) => call[0])).toEqual([
      'Catalog refresh started',
      'Backfill started; follow it on the Batch Jobs page',
      'Avatar download queued',
      'Checking the source for the first strip',
    ]);
  });

  it('reports request failures', () => {
    const { add } = setup();

    add.fail(new Error('Network down'), { input: { source: 's', identifier: 'i' } });

    expect(toast.error).toHaveBeenCalledWith('Could not add the comic: Network down');
  });
});
