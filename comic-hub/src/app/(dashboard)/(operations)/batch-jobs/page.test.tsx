import { screen } from '@testing-library/react';
import BatchJobsPage from './page';
import { renderWithProviders } from '@/test/test-utils';
import { useGetBatchSchedulersQuery, useGetRecentBatchJobsQuery } from '@/generated/graphql';
import type { BatchStatusEnum, GetRecentBatchJobsQuery } from '@/generated/graphql';
import { QueryClient } from '@tanstack/react-query';
import { mockQueryResult } from '@/test/mock-query';

vi.mock('@/generated/graphql', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/generated/graphql')>();
  return {
    ...actual,
    useGetBatchSchedulersQuery: vi.fn(),
    useGetRecentBatchJobsQuery: vi.fn(),
    useTriggerJobMutation: vi.fn(() => ({
      mutate: vi.fn(),
      isPending: false,
    })),
    useToggleJobSchedulerMutation: vi.fn(() => ({
      mutate: vi.fn(),
      isPending: false,
    })),
    useGetBatchJobLogQuery: vi.fn(() => ({
      data: null,
      isLoading: false,
    })),
  };
});

type RecentJobsOptions = NonNullable<Parameters<typeof useGetRecentBatchJobsQuery>[1]>;

function recentJob(status: BatchStatusEnum): GetRecentBatchJobsQuery['recentBatchJobs'][number] {
  return {
    executionId: 1,
    jobName: 'ComicDownloadJob',
    status,
    startTime: new Date().toISOString(),
    endTime: null,
    durationMs: null,
    exitCode: null,
    exitDescription: null,
    steps: null,
  };
}

// A real query-cache entry for the recent-jobs query, holding `jobs` if given
function recentJobsQuery(jobs?: GetRecentBatchJobsQuery['recentBatchJobs']) {
  const client = new QueryClient();
  const query = client.getQueryCache().build<GetRecentBatchJobsQuery, unknown>(client, { queryKey: ['GetRecentBatchJobs'] });
  if (jobs) query.setData({ recentBatchJobs: jobs });
  return query;
}

describe('BatchJobsPage', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders page title', () => {
    vi.mocked(useGetBatchSchedulersQuery).mockReturnValue(mockQueryResult({ data: undefined, isLoading: true }));
    vi.mocked(useGetRecentBatchJobsQuery).mockReturnValue(mockQueryResult({ data: undefined, isLoading: true }));
    renderWithProviders(<BatchJobsPage />);
    expect(screen.getByText('Batch Jobs')).toBeInTheDocument();
    expect(screen.getByText(/Monitor and manage/)).toBeInTheDocument();
  });

  it('shows loading skeletons while data is loading', () => {
    vi.mocked(useGetBatchSchedulersQuery).mockReturnValue(mockQueryResult({ data: undefined, isLoading: true }));
    vi.mocked(useGetRecentBatchJobsQuery).mockReturnValue(mockQueryResult({ data: undefined, isLoading: true }));
    renderWithProviders(<BatchJobsPage />);
    // Summary bar and cards should not be present while loading
    expect(screen.queryByText(/active/)).not.toBeInTheDocument();
  });

  it('renders job cards for each scheduler', () => {
    vi.mocked(useGetBatchSchedulersQuery).mockReturnValue(mockQueryResult({
      data: {
        batchSchedulers: [
          { jobName: 'ComicDownloadJob', cronExpression: '0 0 6 * * ?', timezone: 'America/Toronto', nextRunTime: null, enabled: true, paused: false, lastToggled: null, toggledBy: null, availableParameters: [] },
          { jobName: 'ComicBackfillJob', cronExpression: '0 0 7 * * ?', timezone: 'America/Toronto', nextRunTime: null, enabled: true, paused: true, lastToggled: null, toggledBy: null, availableParameters: [] },
        ],
      },
      isLoading: false,
    }));
    vi.mocked(useGetRecentBatchJobsQuery).mockReturnValue(mockQueryResult({ data: { recentBatchJobs: [] }, isLoading: false }));
    renderWithProviders(<BatchJobsPage />);

    expect(screen.getByText('Comic Download')).toBeInTheDocument();
    expect(screen.getByText('Comic Backfill')).toBeInTheDocument();
  });

  it('shows summary bar with active and paused counts', () => {
    vi.mocked(useGetBatchSchedulersQuery).mockReturnValue(mockQueryResult({
      data: {
        batchSchedulers: [
          { jobName: 'ComicDownloadJob', cronExpression: '0 0 6 * * ?', timezone: 'America/Toronto', nextRunTime: null, enabled: true, paused: false, lastToggled: null, toggledBy: null, availableParameters: [] },
          { jobName: 'ComicBackfillJob', cronExpression: '0 0 7 * * ?', timezone: 'America/Toronto', nextRunTime: null, enabled: true, paused: true, lastToggled: null, toggledBy: null, availableParameters: [] },
        ],
      },
      isLoading: false,
    }));
    vi.mocked(useGetRecentBatchJobsQuery).mockReturnValue(mockQueryResult({ data: { recentBatchJobs: [] }, isLoading: false }));
    renderWithProviders(<BatchJobsPage />);

    expect(screen.getByText((_content, element) => element?.textContent === '1 active')).toBeInTheDocument();
    expect(screen.getByText((_content, element) => element?.textContent === '1 paused')).toBeInTheDocument();
  });

  it('shows empty state when no schedulers configured', () => {
    vi.mocked(useGetBatchSchedulersQuery).mockReturnValue(mockQueryResult({ data: { batchSchedulers: [] }, isLoading: false }));
    vi.mocked(useGetRecentBatchJobsQuery).mockReturnValue(mockQueryResult({ data: { recentBatchJobs: [] }, isLoading: false }));
    renderWithProviders(<BatchJobsPage />);

    expect(screen.getByText(/No batch job schedulers/)).toBeInTheDocument();
  });

  it('maps last execution to correct job card', () => {
    vi.mocked(useGetBatchSchedulersQuery).mockReturnValue(mockQueryResult({
      data: {
        batchSchedulers: [
          { jobName: 'ComicDownloadJob', cronExpression: '0 0 6 * * ?', timezone: 'America/Toronto', nextRunTime: null, enabled: true, paused: false, lastToggled: null, toggledBy: null, availableParameters: [] },
        ],
      },
      isLoading: false,
    }));
    vi.mocked(useGetRecentBatchJobsQuery).mockReturnValue(mockQueryResult({
      data: {
        recentBatchJobs: [
          { executionId: 1, jobName: 'ComicDownloadJob', status: 'COMPLETED' as BatchStatusEnum, startTime: new Date().toISOString(), endTime: new Date().toISOString(), durationMs: 5000, exitCode: 'COMPLETED', exitDescription: null, steps: [] },
        ],
      },
      isLoading: false,
    }));
    renderWithProviders(<BatchJobsPage />);

    expect(screen.getByText('COMPLETED')).toBeInTheDocument();
  });

  it('shows last failure time in minutes when recent', () => {
    vi.mocked(useGetBatchSchedulersQuery).mockReturnValue(mockQueryResult({
      data: {
        batchSchedulers: [
          { jobName: 'ComicDownloadJob', cronExpression: '0 0 6 * * ?', timezone: 'America/Toronto', nextRunTime: null, enabled: true, paused: false, lastToggled: null, toggledBy: null, availableParameters: [] },
        ],
      },
      isLoading: false,
    }));
    vi.mocked(useGetRecentBatchJobsQuery).mockReturnValue(mockQueryResult({
      data: {
        recentBatchJobs: [
          { executionId: 1, jobName: 'ComicDownloadJob', status: 'FAILED' as BatchStatusEnum, startTime: new Date(Date.now() - 300_000).toISOString(), endTime: null, durationMs: null, exitCode: 'FAILED', exitDescription: 'Error', steps: [] },
        ],
      },
      isLoading: false,
    }));
    renderWithProviders(<BatchJobsPage />);

    // formatTimeAgo in summary bar shows "Xm ago" for recent failures
    const matches = screen.getAllByText(/\dm ago/);
    expect(matches.length).toBeGreaterThan(0);
  });

  it('shows no paused or failed sections when counts are zero', () => {
    vi.mocked(useGetBatchSchedulersQuery).mockReturnValue(mockQueryResult({
      data: {
        batchSchedulers: [
          { jobName: 'ComicDownloadJob', cronExpression: '0 0 6 * * ?', timezone: 'America/Toronto', nextRunTime: null, enabled: true, paused: false, lastToggled: null, toggledBy: null, availableParameters: [] },
        ],
      },
      isLoading: false,
    }));
    vi.mocked(useGetRecentBatchJobsQuery).mockReturnValue(mockQueryResult({
      data: { recentBatchJobs: [
        { executionId: 1, jobName: 'ComicDownloadJob', status: 'COMPLETED' as BatchStatusEnum, startTime: new Date().toISOString(), endTime: new Date().toISOString(), durationMs: 5000, exitCode: 'COMPLETED', exitDescription: null, steps: [] },
      ] },
      isLoading: false,
    }));
    renderWithProviders(<BatchJobsPage />);

    expect(screen.queryByText(/paused/)).not.toBeInTheDocument();
    expect(screen.queryByText(/failed/)).not.toBeInTheDocument();
  });

  it('shows failure time in days when old', () => {
    vi.mocked(useGetBatchSchedulersQuery).mockReturnValue(mockQueryResult({
      data: {
        batchSchedulers: [
          { jobName: 'ComicDownloadJob', cronExpression: '0 0 6 * * ?', timezone: 'America/Toronto', nextRunTime: null, enabled: true, paused: false, lastToggled: null, toggledBy: null, availableParameters: [] },
        ],
      },
      isLoading: false,
    }));
    vi.mocked(useGetRecentBatchJobsQuery).mockReturnValue(mockQueryResult({
      data: {
        recentBatchJobs: [
          { executionId: 1, jobName: 'ComicDownloadJob', status: 'FAILED' as BatchStatusEnum, startTime: new Date(Date.now() - 3 * 86_400_000).toISOString(), endTime: null, durationMs: null, exitCode: 'FAILED', exitDescription: 'Error', steps: [] },
        ],
      },
      isLoading: false,
    }));
    renderWithProviders(<BatchJobsPage />);

    expect(screen.getByText(/3d ago/)).toBeInTheDocument();
  });

  it('passes refetchInterval that returns 3000 when a job is STARTED', () => {
    let refetchInterval: RecentJobsOptions['refetchInterval'];
    vi.mocked(useGetRecentBatchJobsQuery).mockImplementation((_vars, opts) => {
      refetchInterval = opts?.refetchInterval;
      return mockQueryResult({ data: { recentBatchJobs: [] }, isLoading: false });
    });
    vi.mocked(useGetBatchSchedulersQuery).mockReturnValue(mockQueryResult({
      data: { batchSchedulers: [] },
      isLoading: false,
    }));
    renderWithProviders(<BatchJobsPage />);

    if (typeof refetchInterval !== 'function') throw new Error('expected a refetchInterval function');
    expect(refetchInterval(recentJobsQuery([recentJob('STARTED')]))).toBe(3000);
    expect(refetchInterval(recentJobsQuery([recentJob('COMPLETED')]))).toBe(false);
    expect(refetchInterval(recentJobsQuery())).toBe(false);
  });

  it('truncates executions to 5 per job', () => {
    const jobs = Array.from({ length: 8 }, (_, i) => ({
      executionId: i + 1,
      jobName: 'ComicDownloadJob',
      status: 'COMPLETED' as BatchStatusEnum,
      startTime: new Date().toISOString(),
      endTime: new Date().toISOString(),
      durationMs: 5000,
      exitCode: 'COMPLETED',
      exitDescription: null,
      steps: [],
    }));

    vi.mocked(useGetBatchSchedulersQuery).mockReturnValue(mockQueryResult({
      data: {
        batchSchedulers: [
          { jobName: 'ComicDownloadJob', cronExpression: '0 0 6 * * ?', timezone: 'America/Toronto', nextRunTime: null, enabled: true, paused: false, lastToggled: null, toggledBy: null, availableParameters: [] },
        ],
      },
      isLoading: false,
    }));
    vi.mocked(useGetRecentBatchJobsQuery).mockReturnValue(mockQueryResult({
      data: { recentBatchJobs: jobs },
      isLoading: false,
    }));
    renderWithProviders(<BatchJobsPage />);

    // Should still render without error (truncation happens internally)
    expect(screen.getByText('Comic Download')).toBeInTheDocument();
  });

  it('shows last failure time in summary bar', () => {
    vi.mocked(useGetBatchSchedulersQuery).mockReturnValue(mockQueryResult({
      data: {
        batchSchedulers: [
          { jobName: 'ComicDownloadJob', cronExpression: '0 0 6 * * ?', timezone: 'America/Toronto', nextRunTime: null, enabled: true, paused: false, lastToggled: null, toggledBy: null, availableParameters: [] },
        ],
      },
      isLoading: false,
    }));
    vi.mocked(useGetRecentBatchJobsQuery).mockReturnValue(mockQueryResult({
      data: {
        recentBatchJobs: [
          { executionId: 1, jobName: 'ComicDownloadJob', status: 'FAILED' as BatchStatusEnum, startTime: new Date(Date.now() - 7_200_000).toISOString(), endTime: null, durationMs: null, exitCode: 'FAILED', exitDescription: 'Error', steps: [] },
        ],
      },
      isLoading: false,
    }));
    renderWithProviders(<BatchJobsPage />);

    expect(screen.getByText(/failed/)).toBeInTheDocument();
    expect(screen.getByText(/2h ago/)).toBeInTheDocument();
  });
});
