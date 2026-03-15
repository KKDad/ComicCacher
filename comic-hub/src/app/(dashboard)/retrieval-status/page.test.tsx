import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import RetrievalStatusPage from './page';
import { useGetRetrievalSummaryQuery, useGetRetrievalRecordsQuery, RetrievalStatusEnum } from '@/generated/graphql';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

vi.mock('@/generated/graphql', () => ({
  useGetRetrievalSummaryQuery: vi.fn(),
  useGetRetrievalRecordsQuery: vi.fn(),
  RetrievalStatusEnum: {
    Success: 'SUCCESS',
    Failure: 'FAILURE',
    Error: 'ERROR',
    Skipped: 'SKIPPED',
    RateLimited: 'RATE_LIMITED',
    NotFound: 'NOT_FOUND',
  },
}));

const mockSummary = {
  retrievalSummary: {
    totalAttempts: 1500,
    successCount: 1350,
    failureCount: 100,
    skippedCount: 30,
    successRate: 90.0,
    averageDurationMs: 245.5,
    byStatus: [
      { status: RetrievalStatusEnum.Success, count: 1350 },
      { status: RetrievalStatusEnum.Failure, count: 100 },
      { status: RetrievalStatusEnum.Skipped, count: 30 },
      { status: RetrievalStatusEnum.NotFound, count: 20 },
    ],
    byComic: [
      { comicName: 'Garfield', totalAttempts: 500, successCount: 490, failureCount: 10 },
      { comicName: 'Peanuts', totalAttempts: 400, successCount: 380, failureCount: 20 },
    ],
  },
};

const mockRecords = {
  retrievalRecords: [
    {
      id: 'Garfield_2024-01-15',
      comicName: 'Garfield',
      comicDate: '2024-01-15',
      source: 'gocomics',
      status: RetrievalStatusEnum.Success,
      retrievalDurationMs: 150.5,
      imageSize: 52428,
      httpStatusCode: 200,
      errorMessage: null,
    },
    {
      id: 'Peanuts_2024-01-15',
      comicName: 'Peanuts',
      comicDate: '2024-01-15',
      source: 'gocomics',
      status: RetrievalStatusEnum.Failure,
      retrievalDurationMs: 3200,
      imageSize: null,
      httpStatusCode: 500,
      errorMessage: 'Connection timeout',
    },
    {
      id: 'Calvin_2024-01-14',
      comicName: 'Calvin and Hobbes',
      comicDate: '2024-01-14',
      source: 'gocomics',
      status: RetrievalStatusEnum.NotFound,
      retrievalDurationMs: null,
      imageSize: null,
      httpStatusCode: 404,
      errorMessage: null,
    },
  ],
};

function renderWithQuery(ui: React.ReactElement) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={qc}>{ui}</QueryClientProvider>);
}

describe('RetrievalStatusPage', () => {
  beforeEach(() => {
    vi.mocked(useGetRetrievalSummaryQuery).mockReturnValue({
      data: mockSummary,
      isLoading: false,
      error: null,
    } as any);
    vi.mocked(useGetRetrievalRecordsQuery).mockReturnValue({
      data: mockRecords,
      isLoading: false,
      error: null,
    } as any);
  });

  it('renders loading skeletons when loading', () => {
    vi.mocked(useGetRetrievalSummaryQuery).mockReturnValue({
      data: undefined,
      isLoading: true,
      error: null,
    } as any);
    vi.mocked(useGetRetrievalRecordsQuery).mockReturnValue({
      data: undefined,
      isLoading: true,
      error: null,
    } as any);
    const { container } = renderWithQuery(<RetrievalStatusPage />);
    const skeletons = container.querySelectorAll('[class*="animate-pulse"], [data-slot="skeleton"]');
    expect(skeletons.length).toBeGreaterThan(0);
  });

  it('renders error state', () => {
    vi.mocked(useGetRetrievalSummaryQuery).mockReturnValue({
      data: undefined,
      isLoading: false,
      error: new Error('Network error'),
    } as any);
    renderWithQuery(<RetrievalStatusPage />);
    expect(screen.getByText('Failed to load retrieval status')).toBeInTheDocument();
    expect(screen.getByText('Network error')).toBeInTheDocument();
  });

  it('renders error state with non-Error object', () => {
    vi.mocked(useGetRetrievalSummaryQuery).mockReturnValue({
      data: undefined,
      isLoading: false,
      error: 'some string error',
    } as any);
    renderWithQuery(<RetrievalStatusPage />);
    expect(screen.getByText('Failed to load retrieval status')).toBeInTheDocument();
    expect(screen.getByText('An unexpected error occurred.')).toBeInTheDocument();
  });

  it('renders empty state when no summary data', () => {
    vi.mocked(useGetRetrievalSummaryQuery).mockReturnValue({
      data: { retrievalSummary: null },
      isLoading: false,
      error: null,
    } as any);
    renderWithQuery(<RetrievalStatusPage />);
    expect(screen.getByText('Retrieval Status')).toBeInTheDocument();
    expect(screen.getByText('No retrieval data available')).toBeInTheDocument();
  });

  it('renders summary cards with correct values', () => {
    renderWithQuery(<RetrievalStatusPage />);
    expect(screen.getByText('Total Attempts')).toBeInTheDocument();
    expect(screen.getByText('1,500')).toBeInTheDocument();
    expect(screen.getByText('Success Rate')).toBeInTheDocument();
    expect(screen.getByText('90.0%')).toBeInTheDocument();
    expect(screen.getByText('Avg Duration')).toBeInTheDocument();
    expect(screen.getByText('246 ms')).toBeInTheDocument();
  });

  it('renders status breakdown badges', () => {
    renderWithQuery(<RetrievalStatusPage />);
    expect(screen.getByText('Status Breakdown')).toBeInTheDocument();
    const breakdownCard = screen.getByText('Status Breakdown').closest('[class*="card"]')!;
    expect(within(breakdownCard).getByText('1,350')).toBeInTheDocument();
  });

  it('renders records table with data', () => {
    renderWithQuery(<RetrievalStatusPage />);
    expect(screen.getByText('Retrieval Records')).toBeInTheDocument();
    expect(screen.getByText('Garfield')).toBeInTheDocument();
    expect(screen.getByText('Peanuts')).toBeInTheDocument();
    expect(screen.getByText('Calvin and Hobbes')).toBeInTheDocument();
  });

  it('renders status badges with correct text', () => {
    renderWithQuery(<RetrievalStatusPage />);
    const table = screen.getByText('Retrieval Records').closest('[class*="card"]')!;
    expect(within(table).getByText('SUCCESS')).toBeInTheDocument();
    expect(within(table).getByText('FAILURE')).toBeInTheDocument();
    expect(within(table).getByText('NOT FOUND')).toBeInTheDocument();
  });

  it('shows error message in record row', () => {
    renderWithQuery(<RetrievalStatusPage />);
    expect(screen.getByText('Connection timeout')).toBeInTheDocument();
  });

  it('shows dash for null duration', () => {
    renderWithQuery(<RetrievalStatusPage />);
    const table = screen.getByText('Retrieval Records').closest('[class*="card"]')!;
    const calvinRow = within(table).getByText('Calvin and Hobbes').closest('tr')!;
    const cells = calvinRow.querySelectorAll('td');
    expect(cells[4].textContent).toBe('—');
  });

  it('shows dash for null image size', () => {
    renderWithQuery(<RetrievalStatusPage />);
    const table = screen.getByText('Retrieval Records').closest('[class*="card"]')!;
    const peanutsRow = within(table).getByText('Peanuts').closest('tr')!;
    const cells = peanutsRow.querySelectorAll('td');
    expect(cells[5].textContent).toBe('—');
  });

  it('formats image size in bytes', () => {
    renderWithQuery(<RetrievalStatusPage />);
    expect(screen.getByText('51.2 KB')).toBeInTheDocument();
  });

  it('formats duration in seconds for large values', () => {
    renderWithQuery(<RetrievalStatusPage />);
    expect(screen.getByText('3.2 s')).toBeInTheDocument();
  });

  it('sorts records by comic name when header clicked', async () => {
    const user = userEvent.setup();
    renderWithQuery(<RetrievalStatusPage />);
    const table = screen.getByText('Retrieval Records').closest('[class*="card"]')!;
    const comicSortBtn = within(table).getAllByRole('button').find(
      (btn) => btn.textContent?.includes('Comic')
    )!;

    // First click: comicName desc → P, G, C
    await user.click(comicSortBtn);
    let rows = within(table).getAllByRole('row').slice(1);
    expect(rows[0]).toHaveTextContent('Peanuts');
    expect(rows[2]).toHaveTextContent('Calvin and Hobbes');

    // Second click: comicName asc → C, G, P
    await user.click(comicSortBtn);
    rows = within(table).getAllByRole('row').slice(1);
    expect(rows[0]).toHaveTextContent('Calvin and Hobbes');
    expect(rows[2]).toHaveTextContent('Peanuts');
  });

  it('sorts records by status when header clicked', async () => {
    const user = userEvent.setup();
    renderWithQuery(<RetrievalStatusPage />);
    const table = screen.getByText('Retrieval Records').closest('[class*="card"]')!;
    const statusSortBtn = within(table).getAllByRole('button').find(
      (btn) => btn.textContent?.includes('Status')
    )!;

    await user.click(statusSortBtn);
    const rows = within(table).getAllByRole('row').slice(1);
    // desc: SUCCESS, NOT_FOUND, FAILURE
    expect(rows[0]).toHaveTextContent('Garfield');
  });

  it('sorts records by duration when header clicked', async () => {
    const user = userEvent.setup();
    renderWithQuery(<RetrievalStatusPage />);
    const table = screen.getByText('Retrieval Records').closest('[class*="card"]')!;
    const durationSortBtn = within(table).getAllByRole('button').find(
      (btn) => btn.textContent?.includes('Duration')
    )!;

    await user.click(durationSortBtn);
    const rows = within(table).getAllByRole('row').slice(1);
    // desc: Peanuts (3200), Garfield (150.5), Calvin (null→0)
    expect(rows[0]).toHaveTextContent('Peanuts');
    expect(rows[2]).toHaveTextContent('Calvin and Hobbes');
  });

  it('shows dash for null averageDurationMs in summary', () => {
    vi.mocked(useGetRetrievalSummaryQuery).mockReturnValue({
      data: {
        retrievalSummary: {
          ...mockSummary.retrievalSummary,
          averageDurationMs: null,
        },
      },
      isLoading: false,
      error: null,
    } as any);
    renderWithQuery(<RetrievalStatusPage />);
    const cards = screen.getByText('Avg Duration').closest('[class*="card"]')!;
    expect(within(cards).getByText('—')).toBeInTheDocument();
  });
});
