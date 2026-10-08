import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { DayOutcome, RetrievalStatusEnum } from '@/generated/graphql';
import { comic, days, record, TARGET_DATE } from '@/test/retrieval-health';
import { ResultsGrid } from './results-grid';

const MISSING_DAY = '2026-10-07';

function grid(onSelect = vi.fn()) {
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
  render(<ResultsGrid comics={comics} dates={comics[0].days.map((d) => d.date)} onSelect={onSelect} />);
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

  it('shows the streak, the last error and a reader link', () => {
    grid();

    expect(screen.getByRole('link', { name: 'Garfield' })).toHaveAttribute('href', '/comics/1/read');
    expect(screen.getByText('RATE LIMITED')).toBeInTheDocument();
    expect(screen.getByText('RATE_LIMITED message')).toBeInTheDocument();
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
});
