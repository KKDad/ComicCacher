import { screen } from '@testing-library/react';
import BatchJobsPage from './page';
import { renderWithProviders } from '@/test/test-utils';
import { useGetBatchSchedulersQuery, useGetRecentBatchJobsQuery } from '@/generated/graphql';
import type { BatchStatusEnum } from '@/generated/graphql';

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

describe('BatchJobsPage', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders page title', () => {
    vi.mocked(useGetBatchSchedulersQuery).mockReturnValue({ data: undefined, isLoading: true } as any);
    vi.mocked(useGetRecentBatchJobsQuery).mockReturnValue({ data: undefined, isLoading: true } as any);
    renderWithProviders(<BatchJobsPage />);
    expect(screen.getByText('Batch Jobs')).toBeInTheDocument();
    expect(screen.getByText(/Monitor and manage/)).toBeInTheDocument();
  });

  it('shows loading skeletons while data is loading', () => {
    vi.mocked(useGetBatchSchedulersQuery).mockReturnValue({ data: undefined, isLoading: true } as any);
    vi.mocked(useGetRecentBatchJobsQuery).mockReturnValue({ data: undefined, isLoading: true } as any);
    renderWithProviders(<BatchJobsPage />);
    // Summary bar and cards should not be present while loading
    expect(screen.queryByText(/active/)).not.toBeInTheDocument();
  });

  it('renders job cards for each scheduler', () => {
    vi.mocked(useGetBatchSchedulersQuery).mockReturnValue({
      data: {
        batchSchedulers: [
          { jobName: 'ComicDownloadJob', cronExpression: '0 0 6 * * ?', timezone: 'America/Toronto', nextRunTime: null, enabled: true, paused: false, lastToggled: null, toggledBy: null },
          { jobName: 'ComicBackfillJob', cronExpression: '0 0 7 * * ?', timezone: 'America/Toronto', nextRunTime: null, enabled: true, paused: true, lastToggled: null, toggledBy: null },
        ],
      },
      isLoading: false,
    } as any);
    vi.mocked(useGetRecentBatchJobsQuery).mockReturnValue({ data: { recentBatchJobs: [] }, isLoading: false } as any);
    renderWithProviders(<BatchJobsPage />);

    expect(screen.getByText('Comic Download')).toBeInTheDocument();
    expect(screen.getByText('Comic Backfill')).toBeInTheDocument();
  });

  it('shows summary bar with active and paused counts', () => {
    vi.mocked(useGetBatchSchedulersQuery).mockReturnValue({
      data: {
        batchSchedulers: [
          { jobName: 'ComicDownloadJob', cronExpression: '0 0 6 * * ?', timezone: 'America/Toronto', nextRunTime: null, enabled: true, paused: false, lastToggled: null, toggledBy: null },
          { jobName: 'ComicBackfillJob', cronExpression: '0 0 7 * * ?', timezone: 'America/Toronto', nextRunTime: null, enabled: true, paused: true, lastToggled: null, toggledBy: null },
        ],
      },
      isLoading: false,
    } as any);
    vi.mocked(useGetRecentBatchJobsQuery).mockReturnValue({ data: { recentBatchJobs: [] }, isLoading: false } as any);
    renderWithProviders(<BatchJobsPage />);

    expect(screen.getByText((_content, element) => element?.textContent === '1 active')).toBeInTheDocument();
    expect(screen.getByText((_content, element) => element?.textContent === '1 paused')).toBeInTheDocument();
  });

  it('shows empty state when no schedulers configured', () => {
    vi.mocked(useGetBatchSchedulersQuery).mockReturnValue({ data: { batchSchedulers: [] }, isLoading: false } as any);
    vi.mocked(useGetRecentBatchJobsQuery).mockReturnValue({ data: { recentBatchJobs: [] }, isLoading: false } as any);
    renderWithProviders(<BatchJobsPage />);

    expect(screen.getByText(/No batch job schedulers/)).toBeInTheDocument();
  });

  it('maps last execution to correct job card', () => {
    vi.mocked(useGetBatchSchedulersQuery).mockReturnValue({
      data: {
        batchSchedulers: [
          { jobName: 'ComicDownloadJob', cronExpression: '0 0 6 * * ?', timezone: 'America/Toronto', nextRunTime: null, enabled: true, paused: false, lastToggled: null, toggledBy: null },
        ],
      },
      isLoading: false,
    } as any);
    vi.mocked(useGetRecentBatchJobsQuery).mockReturnValue({
      data: {
        recentBatchJobs: [
          { executionId: 1, jobName: 'ComicDownloadJob', status: 'COMPLETED' as BatchStatusEnum, startTime: new Date().toISOString(), endTime: new Date().toISOString(), durationMs: 5000, exitCode: 'COMPLETED', exitDescription: null, steps: [] },
        ],
      },
      isLoading: false,
    } as any);
    renderWithProviders(<BatchJobsPage />);

    expect(screen.getByText('COMPLETED')).toBeInTheDocument();
  });

  it('shows last failure time in minutes when recent', () => {
    vi.mocked(useGetBatchSchedulersQuery).mockReturnValue({
      data: {
        batchSchedulers: [
          { jobName: 'ComicDownloadJob', cronExpression: '0 0 6 * * ?', timezone: 'America/Toronto', nextRunTime: null, enabled: true, paused: false, lastToggled: null, toggledBy: null },
        ],
      },
      isLoading: false,
    } as any);
    vi.mocked(useGetRecentBatchJobsQuery).mockReturnValue({
      data: {
        recentBatchJobs: [
          { executionId: 1, jobName: 'ComicDownloadJob', status: 'FAILED' as BatchStatusEnum, startTime: new Date(Date.now() - 300_000).toISOString(), endTime: null, durationMs: null, exitCode: 'FAILED', exitDescription: 'Error', steps: [] },
        ],
      },
      isLoading: false,
    } as any);
    renderWithProviders(<BatchJobsPage />);

    // formatTimeAgo in summary bar shows "Xm ago" for recent failures
    const matches = screen.getAllByText(/\dm ago/);
    expect(matches.length).toBeGreaterThan(0);
  });

  it('shows last failure time in summary bar', () => {
    vi.mocked(useGetBatchSchedulersQuery).mockReturnValue({
      data: {
        batchSchedulers: [
          { jobName: 'ComicDownloadJob', cronExpression: '0 0 6 * * ?', timezone: 'America/Toronto', nextRunTime: null, enabled: true, paused: false, lastToggled: null, toggledBy: null },
        ],
      },
      isLoading: false,
    } as any);
    vi.mocked(useGetRecentBatchJobsQuery).mockReturnValue({
      data: {
        recentBatchJobs: [
          { executionId: 1, jobName: 'ComicDownloadJob', status: 'FAILED' as BatchStatusEnum, startTime: new Date(Date.now() - 7_200_000).toISOString(), endTime: null, durationMs: null, exitCode: 'FAILED', exitDescription: 'Error', steps: [] },
        ],
      },
      isLoading: false,
    } as any);
    renderWithProviders(<BatchJobsPage />);

    expect(screen.getByText(/failed/)).toBeInTheDocument();
    expect(screen.getByText(/2h ago/)).toBeInTheDocument();
  });
});
