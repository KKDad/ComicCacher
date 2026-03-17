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

  it('renders RUNNING badge for STARTED status', () => {
    renderWithProviders(
      <JobCard scheduler={createScheduler()} lastExecution={createExecution({ status: 'STARTED' as BatchStatusEnum })} />,
    );
    expect(screen.getByText('RUNNING')).toBeInTheDocument();
  });

  it('calls toggleMutation when switch is clicked', () => {
    const toggleMutate = vi.fn();
    vi.mocked(useToggleJobSchedulerMutation).mockReturnValue({
      mutate: toggleMutate,
      isPending: false,
    } as any);

    renderWithProviders(<JobCard scheduler={createScheduler()} />);
    screen.getByRole('switch').click();

    expect(toggleMutate).toHaveBeenCalledWith({ jobName: 'ComicDownloadJob', paused: true });
  });

  it('calls triggerMutation when confirm dialog Run Now is clicked', async () => {
    const triggerMutate = vi.fn();
    vi.mocked(useTriggerJobMutation).mockReturnValue({
      mutate: triggerMutate,
      isPending: false,
    } as any);

    renderWithProviders(<JobCard scheduler={createScheduler()} />);
    await userEvent.click(screen.getByRole('button', { name: /run now/i }));
    // Now click the confirm button in the dialog
    const buttons = screen.getAllByRole('button', { name: /run now/i });
    await userEvent.click(buttons[buttons.length - 1]);

    expect(triggerMutate).toHaveBeenCalledWith({ jobName: 'ComicDownloadJob' });
  });

  it('closes confirm dialog on Cancel', async () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} />);
    await userEvent.click(screen.getByRole('button', { name: /run now/i }));
    expect(screen.getByText(/This will trigger a manual execution/)).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /cancel/i }));
    await waitFor(() => {
      expect(screen.queryByText(/This will trigger a manual execution/)).not.toBeInTheDocument();
    });
  });

  it('opens log viewer when View Logs is clicked', async () => {
    renderWithProviders(<JobCard scheduler={createScheduler()} lastExecution={createExecution()} />);
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

    renderWithProviders(<JobCard scheduler={createScheduler()} />);
    capturedOpts.onSuccess({ toggleJobScheduler: { errors: [] } });

    expect(toast.success).toHaveBeenCalledWith('ComicDownloadJob paused');
  });

  it('shows toggle error toast on onSuccess with errors', () => {
    let capturedOpts: any;
    vi.mocked(useToggleJobSchedulerMutation).mockImplementation((opts: any) => {
      capturedOpts = opts;
      return { mutate: vi.fn(), isPending: false } as any;
    });

    renderWithProviders(<JobCard scheduler={createScheduler()} />);
    capturedOpts.onSuccess({ toggleJobScheduler: { errors: [{ message: 'Not allowed' }] } });

    expect(toast.error).toHaveBeenCalledWith('Not allowed');
  });

  it('shows toggle error toast on onError', () => {
    let capturedOpts: any;
    vi.mocked(useToggleJobSchedulerMutation).mockImplementation((opts: any) => {
      capturedOpts = opts;
      return { mutate: vi.fn(), isPending: false } as any;
    });

    renderWithProviders(<JobCard scheduler={createScheduler()} />);
    capturedOpts.onError(new Error('Network failure'));

    expect(toast.error).toHaveBeenCalledWith('Failed to toggle scheduler: Network failure');
  });

  it('shows trigger success toast on onSuccess without errors', () => {
    let capturedOpts: any;
    vi.mocked(useTriggerJobMutation).mockImplementation((opts: any) => {
      capturedOpts = opts;
      return { mutate: vi.fn(), isPending: false } as any;
    });

    renderWithProviders(<JobCard scheduler={createScheduler()} />);
    capturedOpts.onSuccess({ triggerJob: { errors: [] } });

    expect(toast.success).toHaveBeenCalledWith('ComicDownloadJob triggered successfully');
  });

  it('shows trigger error toast on onSuccess with errors', () => {
    let capturedOpts: any;
    vi.mocked(useTriggerJobMutation).mockImplementation((opts: any) => {
      capturedOpts = opts;
      return { mutate: vi.fn(), isPending: false } as any;
    });

    renderWithProviders(<JobCard scheduler={createScheduler()} />);
    capturedOpts.onSuccess({ triggerJob: { errors: [{ message: 'Conflict' }] } });

    expect(toast.error).toHaveBeenCalledWith('Conflict');
  });

  it('shows trigger error toast on onError', () => {
    let capturedOpts: any;
    vi.mocked(useTriggerJobMutation).mockImplementation((opts: any) => {
      capturedOpts = opts;
      return { mutate: vi.fn(), isPending: false } as any;
    });

    renderWithProviders(<JobCard scheduler={createScheduler()} />);
    capturedOpts.onError(new Error('Timeout'));

    expect(toast.error).toHaveBeenCalledWith('Failed to trigger job: Timeout');
  });

  it('shows N/A when exitCode is null', async () => {
    renderWithProviders(
      <JobCard scheduler={createScheduler()} lastExecution={createExecution({ exitCode: null })} />,
    );
    await userEvent.click(screen.getByRole('button', { name: /show details/i }));
    expect(screen.getByText('N/A')).toBeInTheDocument();
  });

  it('renders unknown job name as-is when not in JOB_LABELS', () => {
    renderWithProviders(<JobCard scheduler={createScheduler({ jobName: 'CustomJob' })} />);
    expect(screen.getByText('CustomJob')).toBeInTheDocument();
  });

  it('hides next run time when not provided', () => {
    renderWithProviders(<JobCard scheduler={createScheduler({ nextRunTime: null })} />);
    expect(screen.queryByText(/Next run:/)).not.toBeInTheDocument();
  });

  it('hides duration when durationMs is null', () => {
    renderWithProviders(
      <JobCard scheduler={createScheduler()} lastExecution={createExecution({ durationMs: null })} />,
    );
    expect(screen.getByText(/Last run:/)).toBeInTheDocument();
  });
});
