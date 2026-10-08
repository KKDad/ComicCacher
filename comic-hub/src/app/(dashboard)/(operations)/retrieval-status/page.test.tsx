import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useGetRetrievalHealthQuery, DayOutcome, RetrievalStatusEnum } from '@/generated/graphql';
import { mockQueryResult } from '@/test/mock-query';
import { mockRouter, mockSearchParams } from '@/test/mock-next';
import { comic, days, health, record, TARGET_DATE } from '@/test/retrieval-health';
import RetrievalStatusPage from './page';

vi.mock('@/generated/graphql', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/generated/graphql')>()),
  useGetRetrievalHealthQuery: vi.fn(),
}));

vi.mock('@/components/batch-jobs/log-viewer', () => ({ LogViewer: () => null }));

type Query = ReturnType<typeof useGetRetrievalHealthQuery>;

const missing = record(TARGET_DATE, RetrievalStatusEnum.RateLimited, { httpStatusCode: 429, errorMessage: 'Too many requests' });

const comics = [
  comic(1, 'Garfield'),
  comic(2, 'Peanuts', {
    missingStreak: 1,
    latestError: missing,
    days: days(30, { [TARGET_DATE]: { outcome: DayOutcome.Missing, record: missing } }),
  }),
  comic(3, 'Zits', { source: 'comicskingdom', stale: true, newest: '2026-09-30' }),
  comic(4, 'Retired', { active: false, missingStreak: 0, stale: false }),
];

function givenHealth(extra: Parameters<typeof health>[0] = {}) {
  vi.mocked(useGetRetrievalHealthQuery).mockReturnValue(
    mockQueryResult<Query['data']>({ data: { retrievalHealth: health({ comics, ...extra }) }, isLoading: false, error: null }) as Query,
  );
}

function gridRows() {
  const table = screen.getByRole('table');
  return within(table)
    .getAllByRole('rowheader')
    .map((cell) => within(cell).getByRole('link').textContent);
}

describe('RetrievalStatusPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockSearchParams();
  });

  it('shows a skeleton while loading', () => {
    vi.mocked(useGetRetrievalHealthQuery).mockReturnValue(mockQueryResult<Query['data']>({ isLoading: true }) as Query);

    render(<RetrievalStatusPage />);

    expect(screen.queryByText('Retrieval Status')).not.toBeInTheDocument();
  });

  it('shows the error', () => {
    vi.mocked(useGetRetrievalHealthQuery).mockReturnValue(
      mockQueryResult<Query['data']>({ isLoading: false, error: new Error('boom') }) as Query,
    );

    render(<RetrievalStatusPage />);

    expect(screen.getByText('Failed to load retrieval status')).toBeInTheDocument();
    expect(screen.getByText('boom')).toBeInTheDocument();
  });

  it('takes the full width of the page', () => {
    givenHealth();

    const { container } = render(<RetrievalStatusPage />);

    expect(container.querySelector('[data-layout="wide"]')).not.toBeNull();
  });

  it('asks for the 30-day window', () => {
    givenHealth();

    render(<RetrievalStatusPage />);

    expect(useGetRetrievalHealthQuery).toHaveBeenCalledWith({ days: 30 });
  });

  it('lists every comic by name', () => {
    givenHealth();

    render(<RetrievalStatusPage />);

    expect(gridRows()).toEqual(['Garfield', 'Peanuts', 'Retired', 'Zits']);
  });

  it('lists only the comics that need attention, worst first, with attention=1', () => {
    mockSearchParams({ attention: '1' });
    givenHealth();

    render(<RetrievalStatusPage />);

    expect(gridRows()).toEqual(['Peanuts', 'Zits']);
  });

  it('filters by source and name from the URL', () => {
    mockSearchParams({ source: 'gocomics', q: 'pea' });
    givenHealth();

    render(<RetrievalStatusPage />);

    expect(gridRows()).toEqual(['Peanuts']);
  });

  it('shows the window of days from the URL', () => {
    mockSearchParams({ days: '7' });
    givenHealth();

    render(<RetrievalStatusPage />);

    expect(within(screen.getByRole('table')).getAllByRole('columnheader')).toHaveLength(7 + 2);
  });

  it('writes a filter change to the URL', async () => {
    const router = mockRouter();
    givenHealth();

    render(<RetrievalStatusPage />);
    await userEvent.click(screen.getByRole('switch', { name: 'Only comics needing attention' }));

    expect(router.replace).toHaveBeenCalledWith('/?attention=1', { scroll: false });
  });

  it('says so when every comic is up to date', () => {
    mockSearchParams({ attention: '1' });
    givenHealth({ comics: [comic(1, 'Garfield')] });

    render(<RetrievalStatusPage />);

    expect(screen.getByText('Every comic is up to date.')).toBeInTheDocument();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });

  it('opens a comic’s attempts when its row is clicked', async () => {
    givenHealth();

    render(<RetrievalStatusPage />);
    await userEvent.click(screen.getByRole('img', { name: /Peanuts, Oct 8: missing/ }));

    const drawer = await screen.findByRole('dialog');
    expect(within(drawer).getByText('Peanuts')).toBeInTheDocument();
    expect(within(drawer).getByText('Too many requests')).toBeInTheDocument();
    expect(within(drawer).getByText('HTTP 429')).toBeInTheDocument();
  });

  it('shows the empty state without comics', () => {
    givenHealth({ comics: [] });

    render(<RetrievalStatusPage />);

    expect(screen.getByText('No comics yet')).toBeInTheDocument();
  });
});
