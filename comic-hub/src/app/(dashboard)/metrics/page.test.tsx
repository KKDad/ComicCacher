import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import MetricsPage from './page';
import { useGetCombinedMetricsQuery } from '@/generated/graphql';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

vi.mock('@/generated/graphql', () => ({
  useGetCombinedMetricsQuery: vi.fn(),
}));

const mockStorageComics = [
  { comicId: 1, comicName: 'Garfield', totalBytes: 5242880, imageCount: 100 },
  { comicId: 2, comicName: 'Calvin and Hobbes', totalBytes: 10485760, imageCount: 200 },
  { comicId: 3, comicName: 'Peanuts', totalBytes: 1048576, imageCount: 50 },
];

const mockAccessComics = [
  { comicName: 'Garfield', accessCount: 500, averageAccessTimeMs: 12.5, lastAccessed: new Date(Date.now() - 30000).toISOString() },
  { comicName: 'Calvin and Hobbes', accessCount: 300, averageAccessTimeMs: 8.2, lastAccessed: new Date(Date.now() - 7200000).toISOString() },
  { comicName: 'Peanuts', accessCount: 100, averageAccessTimeMs: null, lastAccessed: null },
];

const mockMetricsData = {
  combinedMetrics: {
    lastUpdated: new Date(Date.now() - 600000).toISOString(),
    storage: {
      totalBytes: 16777216,
      comicCount: 3,
      comics: mockStorageComics,
    },
    access: {
      totalAccesses: 900,
      comics: mockAccessComics,
    },
  },
};

function renderWithQuery(ui: React.ReactElement) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={qc}>{ui}</QueryClientProvider>);
}

describe('MetricsPage', () => {
  beforeEach(() => {
    vi.mocked(useGetCombinedMetricsQuery).mockReturnValue({
      data: mockMetricsData,
      isLoading: false,
      error: null,
    } as any);
  });

  it('renders loading skeletons when loading', () => {
    vi.mocked(useGetCombinedMetricsQuery).mockReturnValue({
      data: undefined,
      isLoading: true,
      error: null,
    } as any);
    const { container } = renderWithQuery(<MetricsPage />);
    const skeletons = container.querySelectorAll('[class*="animate-pulse"], [data-slot="skeleton"]');
    expect(skeletons.length).toBeGreaterThan(0);
  });

  it('renders error state', () => {
    vi.mocked(useGetCombinedMetricsQuery).mockReturnValue({
      data: undefined,
      isLoading: false,
      error: new Error('Network error'),
    } as any);
    renderWithQuery(<MetricsPage />);
    expect(screen.getByText('Failed to load metrics')).toBeInTheDocument();
    expect(screen.getByText('Network error')).toBeInTheDocument();
  });

  it('renders error state with non-Error object', () => {
    vi.mocked(useGetCombinedMetricsQuery).mockReturnValue({
      data: undefined,
      isLoading: false,
      error: 'some string error',
    } as any);
    renderWithQuery(<MetricsPage />);
    expect(screen.getByText('Failed to load metrics')).toBeInTheDocument();
    expect(screen.getByText('An unexpected error occurred.')).toBeInTheDocument();
  });

  it('renders empty state when no metrics data', () => {
    vi.mocked(useGetCombinedMetricsQuery).mockReturnValue({
      data: { combinedMetrics: null },
      isLoading: false,
      error: null,
    } as any);
    renderWithQuery(<MetricsPage />);
    expect(screen.getByText('Metrics')).toBeInTheDocument();
    expect(screen.getByText('No metrics available')).toBeInTheDocument();
  });

  it('renders empty state when storage and access are both null', () => {
    vi.mocked(useGetCombinedMetricsQuery).mockReturnValue({
      data: { combinedMetrics: { lastUpdated: null, storage: null, access: null } },
      isLoading: false,
      error: null,
    } as any);
    renderWithQuery(<MetricsPage />);
    expect(screen.getByText('No metrics available')).toBeInTheDocument();
  });

  it('renders summary cards with correct values', () => {
    renderWithQuery(<MetricsPage />);
    expect(screen.getByText('Total Storage')).toBeInTheDocument();
    expect(screen.getByText('16.0 MB')).toBeInTheDocument();
    expect(screen.getByText('Comics Tracked')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByText('Total Images')).toBeInTheDocument();
    expect(screen.getByText('350')).toBeInTheDocument();
    expect(screen.getByText('Total Accesses')).toBeInTheDocument();
    expect(screen.getByText('900')).toBeInTheDocument();
  });

  it('renders last updated timestamp', () => {
    renderWithQuery(<MetricsPage />);
    expect(screen.getByText('Last updated 10m ago')).toBeInTheDocument();
  });

  it('renders combined metrics table with all comic data', () => {
    renderWithQuery(<MetricsPage />);
    expect(screen.getByText('Metrics by Comic')).toBeInTheDocument();
    const table = screen.getByText('Metrics by Comic').closest('[class*="card"]')!;
    expect(within(table).getByText('Garfield')).toBeInTheDocument();
    expect(within(table).getByText('Calvin and Hobbes')).toBeInTheDocument();
    expect(within(table).getByText('Peanuts')).toBeInTheDocument();
    expect(within(table).getByText('12.5 ms')).toBeInTheDocument();
  });

  it('shows dash for null averageAccessTimeMs and lastAccessed', () => {
    renderWithQuery(<MetricsPage />);
    const table = screen.getByText('Metrics by Comic').closest('[class*="card"]')!;
    const peanutsRow = within(table).getByText('Peanuts').closest('tr')!;
    const cells = peanutsRow.querySelectorAll('td');
    // cols: Comic, Images, Storage, Avg Size, Accesses, Avg Response, Last Accessed
    expect(cells[5].textContent).toBe('—');
    expect(cells[6].textContent).toBe('—');
  });

  it('sorts by comic name when header clicked', async () => {
    const user = userEvent.setup();
    renderWithQuery(<MetricsPage />);
    const table = screen.getByText('Metrics by Comic').closest('[class*="card"]')!;
    const comicSortBtn = within(table).getAllByRole('button').find(
      (btn) => btn.textContent?.includes('Comic')
    )!;

    // First click sets key=comicName, dir=desc → Z-A
    await user.click(comicSortBtn);
    let rows = within(table).getAllByRole('row').slice(1);
    expect(rows[0]).toHaveTextContent('Peanuts');
    expect(rows[2]).toHaveTextContent('Calvin and Hobbes');

    // Second click toggles to asc → A-Z
    await user.click(comicSortBtn);
    rows = within(table).getAllByRole('row').slice(1);
    expect(rows[0]).toHaveTextContent('Calvin and Hobbes');
    expect(rows[2]).toHaveTextContent('Peanuts');
  });

  it('toggles sort direction on repeated click', async () => {
    const user = userEvent.setup();
    renderWithQuery(<MetricsPage />);
    const table = screen.getByText('Metrics by Comic').closest('[class*="card"]')!;
    const storageSortBtn = within(table).getAllByRole('button').find(
      (btn) => btn.textContent?.includes('Storage')
    )!;

    // Default is desc by totalBytes — Calvin (10MB), Garfield (5MB), Peanuts (1MB)
    let rows = within(table).getAllByRole('row').slice(1);
    expect(rows[0]).toHaveTextContent('Calvin and Hobbes');

    // Click again to toggle to asc
    await user.click(storageSortBtn);
    rows = within(table).getAllByRole('row').slice(1);
    expect(rows[0]).toHaveTextContent('Peanuts');
  });

  it('sorts by accesses', async () => {
    const user = userEvent.setup();
    renderWithQuery(<MetricsPage />);
    const table = screen.getByText('Metrics by Comic').closest('[class*="card"]')!;
    const accessSortBtn = within(table).getAllByRole('button').find(
      (btn) => btn.textContent?.includes('Accesses')
    )!;

    // Click to sort by accesses desc — Garfield (500), Calvin (300), Peanuts (100)
    await user.click(accessSortBtn);
    let rows = within(table).getAllByRole('row').slice(1);
    expect(rows[0]).toHaveTextContent('Garfield');

    // Toggle to asc
    await user.click(accessSortBtn);
    rows = within(table).getAllByRole('row').slice(1);
    expect(rows[0]).toHaveTextContent('Peanuts');
  });

  it('sorts by avg response time', async () => {
    const user = userEvent.setup();
    renderWithQuery(<MetricsPage />);
    const table = screen.getByText('Metrics by Comic').closest('[class*="card"]')!;
    const avgBtn = within(table).getAllByRole('button').find(
      (btn) => btn.textContent?.includes('Avg Response')
    )!;

    await user.click(avgBtn);
    const rows = within(table).getAllByRole('row').slice(1);
    // desc: Garfield (12.5), Calvin (8.2), Peanuts (null→0)
    expect(rows[0]).toHaveTextContent('Garfield');
    expect(rows[2]).toHaveTextContent('Peanuts');
  });

  it('sorts by image count', async () => {
    const user = userEvent.setup();
    renderWithQuery(<MetricsPage />);
    const table = screen.getByText('Metrics by Comic').closest('[class*="card"]')!;
    const imgBtn = within(table).getAllByRole('button').find(
      (btn) => btn.textContent?.includes('Images')
    )!;

    await user.click(imgBtn);
    const rows = within(table).getAllByRole('row').slice(1);
    // desc: Calvin (200), Garfield (100), Peanuts (50)
    expect(rows[0]).toHaveTextContent('Calvin and Hobbes');
    expect(rows[2]).toHaveTextContent('Peanuts');
  });

  it('shows avg size dash when imageCount is 0', () => {
    vi.mocked(useGetCombinedMetricsQuery).mockReturnValue({
      data: {
        combinedMetrics: {
          lastUpdated: null,
          storage: {
            totalBytes: 0,
            comicCount: 1,
            comics: [{ comicId: 1, comicName: 'Empty', totalBytes: 0, imageCount: 0 }],
          },
          access: { totalAccesses: 0, comics: [] },
        },
      },
      isLoading: false,
      error: null,
    } as any);
    renderWithQuery(<MetricsPage />);
    const table = screen.getByText('Metrics by Comic').closest('[class*="card"]')!;
    const row = within(table).getByText('Empty').closest('tr')!;
    const cells = row.querySelectorAll('td');
    expect(cells[3].textContent).toBe('—');
  });

  it('does not render lastUpdated when null', () => {
    vi.mocked(useGetCombinedMetricsQuery).mockReturnValue({
      data: {
        combinedMetrics: {
          lastUpdated: null,
          storage: { totalBytes: 100, comicCount: 0, comics: [] },
          access: { totalAccesses: 0, comics: [] },
        },
      },
      isLoading: false,
      error: null,
    } as any);
    renderWithQuery(<MetricsPage />);
    expect(screen.queryByText(/Last updated/)).not.toBeInTheDocument();
  });

  it('handles timeAgo for hours', () => {
    vi.mocked(useGetCombinedMetricsQuery).mockReturnValue({
      data: {
        combinedMetrics: {
          lastUpdated: new Date(Date.now() - 3600000).toISOString(),
          storage: { totalBytes: 0, comicCount: 0, comics: [] },
          access: { totalAccesses: 0, comics: [] },
        },
      },
      isLoading: false,
      error: null,
    } as any);
    renderWithQuery(<MetricsPage />);
    expect(screen.getByText('Last updated 1h ago')).toBeInTheDocument();
  });

  it('handles timeAgo for days', () => {
    vi.mocked(useGetCombinedMetricsQuery).mockReturnValue({
      data: {
        combinedMetrics: {
          lastUpdated: new Date(Date.now() - 86400000 * 3).toISOString(),
          storage: { totalBytes: 0, comicCount: 0, comics: [] },
          access: { totalAccesses: 0, comics: [] },
        },
      },
      isLoading: false,
      error: null,
    } as any);
    renderWithQuery(<MetricsPage />);
    expect(screen.getByText('Last updated 3d ago')).toBeInTheDocument();
  });

  it('handles timeAgo for just now', () => {
    vi.mocked(useGetCombinedMetricsQuery).mockReturnValue({
      data: {
        combinedMetrics: {
          lastUpdated: new Date().toISOString(),
          storage: { totalBytes: 0, comicCount: 0, comics: [] },
          access: { totalAccesses: 0, comics: [] },
        },
      },
      isLoading: false,
      error: null,
    } as any);
    renderWithQuery(<MetricsPage />);
    expect(screen.getByText('Last updated just now')).toBeInTheDocument();
  });

  it('merges storage and access rows with different name formats', () => {
    vi.mocked(useGetCombinedMetricsQuery).mockReturnValue({
      data: {
        combinedMetrics: {
          lastUpdated: null,
          storage: {
            totalBytes: 1000,
            comicCount: 2,
            comics: [
              { comicId: 1, comicName: "Sherman'sLagoon", totalBytes: 500, imageCount: 6 },
              { comicId: 2, comicName: 'BabyBlues', totalBytes: 500, imageCount: 5 },
            ],
          },
          access: {
            totalAccesses: 10,
            comics: [
              { comicName: "Sherman's Lagoon", accessCount: 3, averageAccessTimeMs: 2.0, lastAccessed: null },
              { comicName: 'Baby Blues', accessCount: 7, averageAccessTimeMs: 4.0, lastAccessed: null },
            ],
          },
        },
      },
      isLoading: false,
      error: null,
    } as any);
    renderWithQuery(<MetricsPage />);
    const table = screen.getByText('Metrics by Comic').closest('[class*="card"]')!;
    const rows = within(table).getAllByRole('row').slice(1);
    // Should be 2 rows (merged), not 4
    expect(rows).toHaveLength(2);
    // Prefers display name with spaces
    expect(within(table).getByText("Sherman's Lagoon")).toBeInTheDocument();
    expect(within(table).getByText('Baby Blues')).toBeInTheDocument();
  });

  it('creates new row for access-only comic with no matching storage entry', () => {
    vi.mocked(useGetCombinedMetricsQuery).mockReturnValue({
      data: {
        combinedMetrics: {
          lastUpdated: null,
          storage: { totalBytes: 0, comicCount: 0, comics: [] },
          access: {
            totalAccesses: 42,
            comics: [
              { comicName: 'Dilbert', accessCount: 42, averageAccessTimeMs: 5.0, lastAccessed: null },
            ],
          },
        },
      },
      isLoading: false,
      error: null,
    } as any);
    renderWithQuery(<MetricsPage />);
    const table = screen.getByText('Metrics by Comic').closest('[class*="card"]')!;
    expect(within(table).getByText('Dilbert')).toBeInTheDocument();
    const row = within(table).getByText('Dilbert').closest('tr')!;
    const cells = row.querySelectorAll('td');
    // imageCount and totalBytes should be 0 for access-only entry
    expect(cells[1].textContent).toBe('0');
    expect(cells[2].textContent).toBe('0 B');
  });

  it('formats 0 bytes correctly', () => {
    vi.mocked(useGetCombinedMetricsQuery).mockReturnValue({
      data: {
        combinedMetrics: {
          lastUpdated: null,
          storage: { totalBytes: 0, comicCount: 0, comics: [] },
          access: { totalAccesses: 0, comics: [] },
        },
      },
      isLoading: false,
      error: null,
    } as any);
    renderWithQuery(<MetricsPage />);
    expect(screen.getByText('0 B')).toBeInTheDocument();
  });
});
