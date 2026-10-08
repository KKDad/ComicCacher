import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { DayOutcome, RetrievalStatusEnum } from '@/generated/graphql';
import { comic, days, record, TARGET_DATE } from '@/test/retrieval-health';
import { ResultsGrid } from './results-grid';

const MISSING_DAY = '2026-10-07';

function grid(onSelect = vi.fn(), selectedId: number | null = null, onStep = vi.fn()) {
  const missing = record(MISSING_DAY, RetrievalStatusEnum.RateLimited, { httpStatusCode: 429 });
  const comics = [
    comic(1, 'Garfield', {
      missingStreak: 1,
      latestError: missing,
      days: days(3, {
        [MISSING_DAY]: { outcome: DayOutcome.Missing, record: missing },
        [TARGET_DATE]: { outcome: DayOutcome.OffDay },
      }),
    }),
    comic(2, 'Peanuts', {
      days: days(3, { '2026-10-06': { recovered: true, record: record('2026-10-06', RetrievalStatusEnum.NetworkError) } }),
    }),
  ];
  render(
    <ResultsGrid comics={comics} selectedId={selectedId} dates={comics[0].days.map((d) => d.date)} onSelect={onSelect} onStep={onStep} />,
  );
  return onSelect;
}

describe('ResultsGrid', () => {
  it('shows each day as on disk, missing with its reason, or not due', () => {
    grid();

    expect(screen.getByRole('img', { name: 'Garfield, Oct 7: missing: RATE LIMITED (HTTP 429)' })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Garfield, Oct 8: no strip expected' })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Garfield, Oct 6: on disk' })).toBeInTheDocument();
  });

  it('marks a strip a later attempt recovered', () => {
    grid();

    expect(screen.getByRole('img', { name: 'Peanuts, Oct 6: on disk, recovered after NETWORK ERROR' })).toBeInTheDocument();
  });

  it('shows the newest strip and a reader link', () => {
    grid();

    const row = screen.getByRole('rowheader', { name: 'Garfield' }).closest('tr')!;
    expect(screen.getByRole('link', { name: 'Garfield' })).toHaveAttribute('href', '/comics/1/read');
    expect(within(row).getByText('Oct 8')).toBeInTheDocument();
  });

  it('needs only enough width for its day columns', () => {
    grid();

    expect(screen.getByRole('table').style.minWidth).toBe('20.75rem');
  });

  it('selects the comic when its row is clicked', async () => {
    const onSelect = grid();

    await userEvent.click(screen.getByRole('img', { name: 'Peanuts, Oct 8: on disk' }));

    expect(onSelect).toHaveBeenCalledWith(expect.objectContaining({ comicName: 'Peanuts' }));
  });

  it('does not select the comic when the reader link is followed', async () => {
    const onSelect = grid();

    await userEvent.click(screen.getByRole('link', { name: 'Peanuts' }));

    expect(onSelect).not.toHaveBeenCalled();
  });

  it('marks the selected comic’s row', () => {
    grid(vi.fn(), 2);

    expect(screen.getByRole('rowheader', { name: 'Peanuts' }).closest('tr')).toHaveAttribute('aria-current', 'true');
    expect(screen.getByRole('rowheader', { name: 'Garfield' }).closest('tr')).not.toHaveAttribute('aria-current');
  });

  it('steps with the arrow keys while it has focus', async () => {
    const onStep = vi.fn();
    grid(vi.fn(), 1, onStep);

    screen.getByRole('region', { name: /Results by day/ }).focus();
    await userEvent.keyboard('{ArrowDown}{ArrowUp}');

    expect(onStep.mock.calls).toEqual([[1], [-1]]);
  });

  it('leaves the arrow keys alone without focus', async () => {
    const onStep = vi.fn();
    grid(vi.fn(), 1, onStep);

    await userEvent.keyboard('{ArrowDown}');

    expect(onStep).not.toHaveBeenCalled();
  });

  it('names the month on the first column and on each 1st', () => {
    const comics = [comic(1, 'Garfield', { days: days(10) })];
    render(<ResultsGrid comics={comics} dates={comics[0].days.map((d) => d.date)} onSelect={vi.fn()} onStep={vi.fn()} />);

    const headers = screen.getAllByRole('columnheader').slice(1, -1);
    expect(headers[0]).toHaveTextContent(/^Sep/);
    expect(headers.find((h) => h.title === 'Oct 1')).toHaveTextContent(/^Oct/);
    expect(headers.find((h) => h.title === 'Oct 2')).not.toHaveTextContent(/Oct/);
  });
});
