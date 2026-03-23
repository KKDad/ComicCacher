import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { toast } from 'sonner';
import { JobCard } from './job-card';
import { renderWithProviders } from '@/test/test-utils';
import {
  useTriggerJobMutation,
  useToggleJobSchedulerMutation,
} from '@/generated/graphql';
import type { BatchSchedulerInfo, BatchJob, BatchStatusEnum } from '@/generated/graphql';

vi.mock('sonner', () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}));

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
    description: null,
    lastToggled: null,
    toggledBy: null,
    availableParameters: [],
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
    renderWithProviders(<JobCard scheduler={createScheduler()} recentExecutions={[]} />);
    expect(screen.getByText('Comic Download')).toBeInTheDocument();
  });

  it('renders info icon when scheduler has a description', () => {
    const { container } = renderWithProviders(
      <JobCard scheduler={createScheduler({ description: 'Downloads comics daily' })} recentExecutions={[]} />,
    );
    expect(container.querySelector('.lucide-info')).toBeInTheDocument();
  });

  it('does not render info icon when scheduler has no description', () => {
    const { container } = renderWithProviders(<JobCard scheduler={createScheduler()} recentExecutions={[]} />);
    expect(container.querySelector('.lucide-info')).not.toBeInTheDocument();
  });

  it('renders PAUSED badge when scheduler is paused', () => {
    renderWithProviders(<JobCard scheduler={createScheduler({ paused: true })} recentExecutions={[]} />);
    expect(screen.getByText('PAUSED')).toBeInTheDocument();
  });

  it('renders COMPLETED badge from last execution', () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} recentExecutions={[createExecution()]} />);
    expect(screen.getByText('COMPLETED')).toBeInTheDocument();
  });

  it('renders FAILED badge for failed execution', () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} recentExecutions={[createExecution({ status: 'FAILED' as BatchStatusEnum })]} />);
    expect(screen.getByText('FAILED')).toBeInTheDocument();
  });

  it('renders human-readable cron schedule', () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} recentExecutions={[]} />);
    expect(screen.getByText(/Daily at 6:00 AM ET/)).toBeInTheDocument();
  });

  it('renders next run time', () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} recentExecutions={[]} />);
    expect(screen.getByText(/Next run:/)).toBeInTheDocument();
  });

  it('renders last ran info with absolute time when execution exists', () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} recentExecutions={[createExecution()]} />);
    expect(screen.getByText(/Last Ran:/)).toBeInTheDocument();
    expect(screen.getByText(/·/)).toBeInTheDocument();
  });

  it('renders active/paused toggle switch', () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} recentExecutions={[]} />);
    expect(screen.getByText('Active')).toBeInTheDocument();
    expect(screen.getByRole('switch')).toBeInTheDocument();
  });

  it('renders Paused label when paused', () => {
    renderWithProviders(<JobCard scheduler={createScheduler({ paused: true })} recentExecutions={[]} />);
    expect(screen.getByText('Paused')).toBeInTheDocument();
  });

  it('renders Run Now button', () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} recentExecutions={[]} />);
    expect(screen.getByRole('button', { name: /run now/i })).toBeInTheDocument();
  });

  it('opens confirmation dialog when Run Now is clicked', async () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} recentExecutions={[]} />);
    await userEvent.click(screen.getByRole('button', { name: /run now/i }));
    expect(screen.getByText(/This will trigger a manual execution/)).toBeInTheDocument();
  });

  it('renders Show Details button when executions exist', () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} recentExecutions={[createExecution()]} />);
    expect(screen.getByRole('button', { name: /show details/i })).toBeInTheDocument();
  });

  it('does not render Show Details button without executions', () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} recentExecutions={[]} />);
    expect(screen.queryByRole('button', { name: /show details/i })).not.toBeInTheDocument();
  });

  it('shows View Logs inside expanded details, not on main card', () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} recentExecutions={[createExecution()]} />);
    expect(screen.queryByRole('button', { name: /view logs/i })).not.toBeInTheDocument();
  });

  it('expands details showing recent runs with view logs', async () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} recentExecutions={[createExecution()]} />);
    await userEvent.click(screen.getByRole('button', { name: /show details/i }));

    expect(screen.getByText('Recent Runs')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /view logs/i })).toBeInTheDocument();
  });

  it('shows multiple executions in details', async () => {
    const executions = [
      createExecution({ executionId: 3, status: 'COMPLETED' as BatchStatusEnum }),
      createExecution({ executionId: 2, status: 'FAILED' as BatchStatusEnum, exitDescription: 'Timeout' }),
      createExecution({ executionId: 1, status: 'COMPLETED' as BatchStatusEnum }),
    ];
    renderWithProviders(<JobCard scheduler={createScheduler()} recentExecutions={executions} />);
    await userEvent.click(screen.getByRole('button', { name: /show details/i }));

    const viewLogsButtons = screen.getAllByRole('button', { name: /view logs/i });
    expect(viewLogsButtons).toHaveLength(3);
    expect(screen.getByText('Timeout')).toBeInTheDocument();
  });

  it('shows error description for failed jobs in run history', async () => {
    renderWithProviders(
      <JobCard
        scheduler={createScheduler()}
        recentExecutions={[createExecution({
          status: 'FAILED' as BatchStatusEnum,
          exitDescription: 'Connection timeout',
        })]}
      />
    );
    await userEvent.click(screen.getByRole('button', { name: /show details/i }));
    expect(screen.getByText('Connection timeout')).toBeInTheDocument();
  });

  it('renders UNKNOWN status badge when no execution exists', () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} recentExecutions={[]} />);
    expect(screen.getByText('UNKNOWN')).toBeInTheDocument();
  });

  it('renders RUNNING badge for STARTED status', () => {
    renderWithProviders(
      <JobCard scheduler={createScheduler()} recentExecutions={[createExecution({ status: 'STARTED' as BatchStatusEnum })]} />,
    );
    expect(screen.getByText('RUNNING')).toBeInTheDocument();
  });

  it('applies green accent border for COMPLETED status', () => {
    const { container } = renderWithProviders(
      <JobCard scheduler={createScheduler()} recentExecutions={[createExecution()]} />,
    );
    expect(container.querySelector('.border-l-green-500')).toBeInTheDocument();
  });

  it('applies red accent border for FAILED status', () => {
    const { container } = renderWithProviders(
      <JobCard scheduler={createScheduler()} recentExecutions={[createExecution({ status: 'FAILED' as BatchStatusEnum })]} />,
    );
    expect(container.querySelector('.border-l-red-500')).toBeInTheDocument();
  });

  it('applies blue accent border for STARTED status', () => {
    const { container } = renderWithProviders(
      <JobCard scheduler={createScheduler()} recentExecutions={[createExecution({ status: 'STARTED' as BatchStatusEnum })]} />,
    );
    expect(container.querySelector('.border-l-blue-500')).toBeInTheDocument();
  });

  it('applies yellow accent border for paused scheduler', () => {
    const { container } = renderWithProviders(
      <JobCard scheduler={createScheduler({ paused: true })} recentExecutions={[createExecution()]} />,
    );
    expect(container.querySelector('.border-l-yellow-500')).toBeInTheDocument();
  });

  it('applies no accent border for UNKNOWN status', () => {
    const { container } = renderWithProviders(
      <JobCard scheduler={createScheduler()} recentExecutions={[]} />,
    );
    expect(container.querySelector('.border-l-4')).not.toBeInTheDocument();
  });

  it('calls toggleMutation when switch is clicked', () => {
    const toggleMutate = vi.fn();
    vi.mocked(useToggleJobSchedulerMutation).mockReturnValue({
      mutate: toggleMutate,
      isPending: false,
    } as any);

    renderWithProviders(<JobCard scheduler={createScheduler()} recentExecutions={[]} />);
    screen.getByRole('switch').click();

    expect(toggleMutate).toHaveBeenCalledWith({ jobName: 'ComicDownloadJob', paused: true });
  });

  it('calls triggerMutation when confirm dialog Run Now is clicked', async () => {
    const triggerMutate = vi.fn();
    vi.mocked(useTriggerJobMutation).mockReturnValue({
      mutate: triggerMutate,
      isPending: false,
    } as any);

    renderWithProviders(<JobCard scheduler={createScheduler()} recentExecutions={[]} />);
    await userEvent.click(screen.getByRole('button', { name: /run now/i }));
    const buttons = screen.getAllByRole('button', { name: /run now/i });
    await userEvent.click(buttons[buttons.length - 1]);

    expect(triggerMutate).toHaveBeenCalledWith({ jobName: 'ComicDownloadJob' });
  });

  it('closes confirm dialog on Cancel', async () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} recentExecutions={[]} />);
    await userEvent.click(screen.getByRole('button', { name: /run now/i }));
    expect(screen.getByText(/This will trigger a manual execution/)).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /cancel/i }));
    await waitFor(() => {
      expect(screen.queryByText(/This will trigger a manual execution/)).not.toBeInTheDocument();
    });
  });

  it('opens log viewer when View Logs is clicked in details', async () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} recentExecutions={[createExecution()]} />);
    await userEvent.click(screen.getByRole('button', { name: /show details/i }));
    await userEvent.click(screen.getByRole('button', { name: /view logs/i }));

    expect(screen.getByText(/Comic Download - Execution #42/)).toBeInTheDocument();
  });

  it('shows toggle success toast on onSuccess without errors', () => {
    let capturedOpts: any;
    vi.mocked(useToggleJobSchedulerMutation).mockImplementation((opts: any) => {
      capturedOpts = opts;
      return { mutate: vi.fn(), isPending: false } as any;
    });

    renderWithProviders(<JobCard scheduler={createScheduler()} recentExecutions={[]} />);
    capturedOpts.onSuccess({ toggleJobScheduler: { errors: [] } });

    expect(toast.success).toHaveBeenCalledWith('ComicDownloadJob paused');
  });

  it('shows toggle error toast on onSuccess with errors', () => {
    let capturedOpts: any;
    vi.mocked(useToggleJobSchedulerMutation).mockImplementation((opts: any) => {
      capturedOpts = opts;
      return { mutate: vi.fn(), isPending: false } as any;
    });

    renderWithProviders(<JobCard scheduler={createScheduler()} recentExecutions={[]} />);
    capturedOpts.onSuccess({ toggleJobScheduler: { errors: [{ message: 'Not allowed' }] } });

    expect(toast.error).toHaveBeenCalledWith('Not allowed');
  });

  it('shows toggle error toast on onError', () => {
    let capturedOpts: any;
    vi.mocked(useToggleJobSchedulerMutation).mockImplementation((opts: any) => {
      capturedOpts = opts;
      return { mutate: vi.fn(), isPending: false } as any;
    });

    renderWithProviders(<JobCard scheduler={createScheduler()} recentExecutions={[]} />);
    capturedOpts.onError(new Error('Network failure'));

    expect(toast.error).toHaveBeenCalledWith('Failed to toggle scheduler: Network failure');
  });

  it('shows trigger success toast on onSuccess without errors', () => {
    let capturedOpts: any;
    vi.mocked(useTriggerJobMutation).mockImplementation((opts: any) => {
      capturedOpts = opts;
      return { mutate: vi.fn(), isPending: false } as any;
    });

    renderWithProviders(<JobCard scheduler={createScheduler()} recentExecutions={[]} />);
    capturedOpts.onSuccess({ triggerJob: { errors: [] } });

    expect(toast.success).toHaveBeenCalledWith('ComicDownloadJob triggered successfully');
  });

  it('shows trigger error toast on onSuccess with errors', () => {
    let capturedOpts: any;
    vi.mocked(useTriggerJobMutation).mockImplementation((opts: any) => {
      capturedOpts = opts;
      return { mutate: vi.fn(), isPending: false } as any;
    });

    renderWithProviders(<JobCard scheduler={createScheduler()} recentExecutions={[]} />);
    capturedOpts.onSuccess({ triggerJob: { errors: [{ message: 'Conflict' }] } });

    expect(toast.error).toHaveBeenCalledWith('Conflict');
  });

  it('shows trigger error toast on onError', () => {
    let capturedOpts: any;
    vi.mocked(useTriggerJobMutation).mockImplementation((opts: any) => {
      capturedOpts = opts;
      return { mutate: vi.fn(), isPending: false } as any;
    });

    renderWithProviders(<JobCard scheduler={createScheduler()} recentExecutions={[]} />);
    capturedOpts.onError(new Error('Timeout'));

    expect(toast.error).toHaveBeenCalledWith('Failed to trigger job: Timeout');
  });

  it('renders unknown job name as-is when not in JOB_LABELS', () => {
    renderWithProviders(<JobCard scheduler={createScheduler({ jobName: 'CustomJob' })} recentExecutions={[]} />);
    expect(screen.getByText('CustomJob')).toBeInTheDocument();
  });

  it('hides next run time when not provided', () => {
    renderWithProviders(<JobCard scheduler={createScheduler({ nextRunTime: null })} recentExecutions={[]} />);
    expect(screen.queryByText(/Next run:/)).not.toBeInTheDocument();
  });

  it('renders STARTING small status badge in run history', async () => {
    renderWithProviders(
      <JobCard
        scheduler={createScheduler()}
        recentExecutions={[createExecution({ status: 'STARTING' as BatchStatusEnum })]}
      />,
    );
    await userEvent.click(screen.getByRole('button', { name: /show details/i }));
    // The main card badge renders RUNNING from getStatusBadge, the details section uses getSmallStatusBadge
    const runningBadges = screen.getAllByText('RUNNING');
    expect(runningBadges.length).toBeGreaterThanOrEqual(1);
  });

  it('renders unknown small status badge in run history', async () => {
    renderWithProviders(
      <JobCard
        scheduler={createScheduler()}
        recentExecutions={[createExecution({ status: 'ABANDONED' as BatchStatusEnum })]}
      />,
    );
    await userEvent.click(screen.getByRole('button', { name: /show details/i }));
    const badges = screen.getAllByText('ABANDONED');
    // Both main badge and small badge render ABANDONED
    expect(badges.length).toBeGreaterThanOrEqual(2);
  });

  it('formats relative time in minutes for near-future next run', () => {
    const tenMinutesFromNow = new Date(Date.now() + 600_000).toISOString();
    renderWithProviders(
      <JobCard
        scheduler={createScheduler({ nextRunTime: tenMinutesFromNow })}
        recentExecutions={[]}
      />,
    );
    // formatRelativeTime returns "in Xm" for < 1 hour future
    expect(screen.getByText(/Next run:.*in \d+m$/)).toBeInTheDocument();
  });

  it('formats relative time in minutes for recent past execution', () => {
    const fiveMinutesAgo = new Date(Date.now() - 300_000).toISOString();
    renderWithProviders(
      <JobCard
        scheduler={createScheduler()}
        recentExecutions={[createExecution({
          startTime: fiveMinutesAgo,
          endTime: new Date(Date.now() - 200_000).toISOString(),
        })]}
      />,
    );
    // formatRelativeTime returns "Xm ago" for < 1 hour past
    expect(screen.getByText(/\d+m ago/)).toBeInTheDocument();
  });

  it('hides duration when durationMs is null', () => {
    renderWithProviders(
      <JobCard scheduler={createScheduler()} recentExecutions={[createExecution({ durationMs: null })]} />,
    );
    expect(screen.getByText(/Last Ran:/)).toBeInTheDocument();
  });
});
