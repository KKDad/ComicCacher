import { render, screen } from '@testing-library/react';
import { DayOfWeek, DayOutcome, RetrievalStatusEnum } from '@/generated/graphql';
import { comic, days, record } from '@/test/retrieval-health';
import { ComicRetrievalDetail, describeComic } from './comic-retrieval-detail';

describe('ComicRetrievalDetail', () => {
  const missing = record('2026-10-08', RetrievalStatusEnum.NetworkError, { httpStatusCode: 403, errorMessage: 'Forbidden' });
  const peanuts = comic(2, 'Peanuts', {
    publicationDays: [DayOfWeek.Monday, DayOfWeek.Friday],
    missingStreak: 1,
    days: days(5, {
      '2026-10-08': { outcome: DayOutcome.Missing, record: missing },
      '2026-10-07': { outcome: DayOutcome.Missing },
      '2026-10-06': { recovered: true, record: record('2026-10-06', RetrievalStatusEnum.RateLimited) },
    }),
  });

  it('shows the schedule and links', () => {
    render(<ComicRetrievalDetail comic={peanuts} />);

    expect(screen.getByText('MON FRI')).toBeInTheDocument();
    expect(screen.getByText('Missing in a row').nextSibling).toHaveTextContent('1');
    expect(screen.getByRole('link', { name: 'Read' })).toHaveAttribute('href', '/comics/2/read');
    expect(screen.getByRole('link', { name: 'Source' })).toHaveAttribute('href', '/sources/gocomics');
  });

  it('lists the days with a record or a gap, newest first, with the full error', () => {
    render(<ComicRetrievalDetail comic={peanuts} />);

    const items = screen.getAllByRole('listitem');
    expect(items).toHaveLength(3);
    expect(items[0]).toHaveTextContent('Oct 8');
    expect(items[0]).toHaveTextContent('HTTP 403');
    expect(items[0]).toHaveTextContent('Forbidden');
    expect(items[1]).toHaveTextContent('missing: not attempted');
    expect(items[2]).toHaveTextContent('recovered after RATE LIMITED');
  });

  it('says when the window has nothing to show', () => {
    render(<ComicRetrievalDetail comic={comic(1, 'Garfield')} />);

    expect(screen.getByText('No attempts or missing strips in the window.')).toBeInTheDocument();
  });

  it('describes the comic', () => {
    expect(describeComic(comic(1, 'A', { indexed: true, active: false, enabled: false }))).toBe(
      'gocomics · numbered strips · inactive · hidden from readers',
    );
    expect(describeComic(comic(1, 'A'))).toBe('gocomics');
  });
});
