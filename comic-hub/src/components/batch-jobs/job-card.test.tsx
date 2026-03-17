import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { JobCard } from './job-card';
import { renderWithProviders } from '@/test/test-utils';
import type { BatchSchedulerInfo, BatchJob, BatchStatusEnum } from '@/generated/graphql';

vi.mock('@/generated/graphql', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/generated/graphql')>();
  return {
    ...actual,
    useTriggerJobMutation: vi.fn(() => ({
      mutate: vi.fn(),
      isPending: false,
    })),
    useToggleJobSchedulerMutation: vi.fn(() => ({
      mutate: vi.fn(),
      isPending: false,
    })),
    useGetBatchSchedulersQuery: Object.assign(vi.fn(), { getKey: () => ['GetBatchSchedulers'] }),
    useGetRecentBatchJobsQuery: Object.assign(vi.fn(), { getKey: () => ['GetRecentBatchJobs'] }),
    useGetBatchJobLogQuery: vi.fn(() => ({
      data: null,
      isLoading: false,
    })),
  };
});

function createScheduler(overrides?: Partial<BatchSchedulerInfo>): BatchSchedulerInfo {
  return {
    __typename: 'BatchSchedulerInfo',
    jobName: 'ComicDownloadJob',
    cronExpression: '0 0 6 * * ?',
    timezone: 'America/Toronto',
    nextRunTime: new Date(Date.now() + 3_600_000 * 14).toISOString(),
    enabled: true,
    paused: false,
    lastToggled: null,
    toggledBy: null,
    ...overrides,
  };
}

function createExecution(overrides?: Partial<BatchJob>): BatchJob {
  return {
    __typename: 'BatchJob',
    executionId: 42,
    jobName: 'ComicDownloadJob',
    status: 'COMPLETED' as BatchStatusEnum,
    startTime: new Date(Date.now() - 3_600_000).toISOString(),
    endTime: new Date(Date.now() - 3_500_000).toISOString(),
    durationMs: 100_000,
    exitCode: 'COMPLETED',
    exitDescription: null,
    steps: [
      {
        __typename: 'BatchStep',
        stepName: 'comicRetrievalStep',
        status: 'COMPLETED' as BatchStatusEnum,
        readCount: 50,
        writeCount: 50,
        filterCount: 0,
        skipCount: 2,
        commitCount: 50,
        rollbackCount: 0,
        startTime: null,
        endTime: null,
      },
    ],
    ...overrides,
  };
}

describe('JobCard', () => {
  it('renders job name as human-readable label', () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} />);
    expect(screen.getByText('Comic Download')).toBeInTheDocument();
  });

  it('renders PAUSED badge when scheduler is paused', () => {
    renderWithProviders(<JobCard scheduler={createScheduler({ paused: true })} />);
    expect(screen.getByText('PAUSED')).toBeInTheDocument();
  });

  it('renders COMPLETED badge from last execution', () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} lastExecution={createExecution()} />);
    expect(screen.getByText('COMPLETED')).toBeInTheDocument();
  });

  it('renders FAILED badge for failed execution', () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} lastExecution={createExecution({ status: 'FAILED' as BatchStatusEnum })} />);
    expect(screen.getByText('FAILED')).toBeInTheDocument();
  });

  it('renders human-readable cron schedule', () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} />);
    expect(screen.getByText(/Daily at 6:00 AM ET/)).toBeInTheDocument();
  });

  it('renders next run time', () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} />);
    expect(screen.getByText(/Next run:/)).toBeInTheDocument();
  });

  it('renders last run info when execution exists', () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} lastExecution={createExecution()} />);
    expect(screen.getByText(/Last run:/)).toBeInTheDocument();
  });

  it('renders active/paused toggle switch', () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} />);
    expect(screen.getByText('Active')).toBeInTheDocument();
    expect(screen.getByRole('switch')).toBeInTheDocument();
  });

  it('renders Paused label when paused', () => {
    renderWithProviders(<JobCard scheduler={createScheduler({ paused: true })} />);
    expect(screen.getByText('Paused')).toBeInTheDocument();
  });

  it('renders Run Now button', () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} />);
    expect(screen.getByRole('button', { name: /run now/i })).toBeInTheDocument();
  });

  it('opens confirmation dialog when Run Now is clicked', async () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} />);
    await userEvent.click(screen.getByRole('button', { name: /run now/i }));
    expect(screen.getByText(/This will trigger a manual execution/)).toBeInTheDocument();
  });

  it('renders Show Details button when execution exists', () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} lastExecution={createExecution()} />);
    expect(screen.getByRole('button', { name: /show details/i })).toBeInTheDocument();
  });

  it('does not render Show Details button without execution', () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} />);
    expect(screen.queryByRole('button', { name: /show details/i })).not.toBeInTheDocument();
  });

  it('expands details with step info on click', async () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} lastExecution={createExecution()} />);
    await userEvent.click(screen.getByRole('button', { name: /show details/i }));

    expect(screen.getByText('comicRetrievalStep')).toBeInTheDocument();
    expect(screen.getByText('Read: 50')).toBeInTheDocument();
    expect(screen.getByText('Write: 50')).toBeInTheDocument();
    expect(screen.getByText('Skip: 2')).toBeInTheDocument();
  });

  it('shows View Logs button in expanded details', async () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} lastExecution={createExecution()} />);
    await userEvent.click(screen.getByRole('button', { name: /show details/i }));
    expect(screen.getByRole('button', { name: /view logs/i })).toBeInTheDocument();
  });

  it('shows exit code in expanded details', async () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} lastExecution={createExecution()} />);
    await userEvent.click(screen.getByRole('button', { name: /show details/i }));
    expect(screen.getByText('Exit Code')).toBeInTheDocument();
    expect(screen.getAllByText('COMPLETED')).toHaveLength(2); // badge + exit code
  });

  it('shows error description for failed jobs', async () => {
    renderWithProviders(
      <JobCard
        scheduler={createScheduler()}
        lastExecution={createExecution({
          status: 'FAILED' as BatchStatusEnum,
          exitDescription: 'Connection timeout',
        })}
      />
    );
    await userEvent.click(screen.getByRole('button', { name: /show details/i }));
    expect(screen.getByText('Connection timeout')).toBeInTheDocument();
  });

  it('renders UNKNOWN status badge when no execution exists', () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} />);
    expect(screen.getByText('UNKNOWN')).toBeInTheDocument();
  });
});
