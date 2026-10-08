import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { DayOutcome } from '@/generated/graphql';
import { comic, days, health, TARGET_DATE } from '@/test/retrieval-health';
import { LastRunBanner } from './last-run-banner';

vi.mock('@/components/batch-jobs/log-viewer', () => ({
  LogViewer: ({ open, executionId }: { open: boolean; executionId: number }) =>
    open ? <div>log for {executionId}</div> : null,
}));

describe('LastRunBanner', () => {
  const comics = [
    comic(1, 'A'),
    comic(2, 'B', { days: days(3, { [TARGET_DATE]: { outcome: DayOutcome.Missing } }) }),
    comic(3, 'C', { days: days(3, { [TARGET_DATE]: { outcome: DayOutcome.Missing } }) }),
  ];

  it('shows the run and today’s counts', () => {
    render(<LastRunBanner targetDate={TARGET_DATE} lastRun={health().lastRun} comics={comics} />);

    expect(screen.getByText('COMPLETED')).toBeInTheDocument();
    expect(screen.getByText('· took 5m 0s')).toBeInTheDocument();
    expect(screen.getByText('On disk today').nextSibling).toHaveTextContent('1');
    expect(screen.getByText('Missing').nextSibling).toHaveTextContent('2');
    expect(screen.getByRole('link', { name: 'Batch jobs' })).toHaveAttribute('href', '/batch-jobs');
  });

  it('opens the run’s log', async () => {
    render(<LastRunBanner targetDate={TARGET_DATE} lastRun={health().lastRun} comics={comics} />);

    await userEvent.click(screen.getByRole('button', { name: /view log/i }));

    expect(screen.getByText('log for 42')).toBeInTheDocument();
  });

  it('says when the daily download has never run', () => {
    render(<LastRunBanner targetDate={TARGET_DATE} lastRun={null} comics={comics} />);

    expect(screen.getByText('The daily download hasn’t run yet.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /view log/i })).not.toBeInTheDocument();
  });
});
