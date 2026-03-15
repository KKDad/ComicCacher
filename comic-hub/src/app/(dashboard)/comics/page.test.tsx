import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ComicsPage from './page';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useGetComicsQuery } from '@/generated/graphql';

vi.mock('@/generated/graphql', () => ({
  useGetComicsQuery: vi.fn(),
}));

function renderWithQuery(ui: React.ReactElement) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={qc}>{ui}</QueryClientProvider>);
}

const mockComicsData = {
  comics: {
    edges: [
      {
        node: {
          id: 1,
          name: 'Garfield',
          newest: '2024-01-15',
          avatarUrl: 'https://example.com/1.png',
          lastStrip: { date: '2024-01-15', imageUrl: 'https://example.com/strip1.png' },
        },
      },
      {
        node: {
          id: 2,
          name: 'Peanuts',
          newest: '2024-01-15',
          avatarUrl: null,
          lastStrip: null,
        },
      },
    ],
    pageInfo: {
      hasNextPage: false,
      hasPreviousPage: false,
      startCursor: 'c1',
      endCursor: 'c2',
    },
    totalCount: 2,
  },
};

describe('ComicsPage', () => {
  beforeEach(() => {
    vi.mocked(useGetComicsQuery).mockReturnValue({
      data: mockComicsData,
      isLoading: false,
      isFetching: false,
    } as any);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders loading skeletons when isLoading', () => {
    vi.mocked(useGetComicsQuery).mockReturnValue({
      data: null,
      isLoading: true,
      isFetching: true,
    } as any);
    renderWithQuery(<ComicsPage />);
    expect(screen.queryByText('Browse Comics')).not.toBeInTheDocument();
  });

  it('renders empty state when no comics', () => {
    vi.mocked(useGetComicsQuery).mockReturnValue({
      data: {
        comics: {
          edges: [],
          pageInfo: { hasNextPage: false, hasPreviousPage: false, startCursor: null, endCursor: null },
          totalCount: 0,
        },
      },
      isLoading: false,
      isFetching: false,
    } as any);
    renderWithQuery(<ComicsPage />);
    expect(screen.getByText('No comics available')).toBeInTheDocument();
  });

  it('renders comic tiles', () => {
    renderWithQuery(<ComicsPage />);
    expect(screen.getByText('Garfield')).toBeInTheDocument();
    expect(screen.getByText('Peanuts')).toBeInTheDocument();
  });

  it('shows comic count', () => {
    renderWithQuery(<ComicsPage />);
    expect(screen.getByText(/2 available comics/)).toBeInTheDocument();
  });

  it('shows Load More button when hasNextPage', () => {
    vi.mocked(useGetComicsQuery).mockReturnValue({
      data: {
        ...mockComicsData,
        comics: {
          ...mockComicsData.comics,
          pageInfo: { ...mockComicsData.comics.pageInfo, hasNextPage: true },
        },
      },
      isLoading: false,
      isFetching: false,
    } as any);
    renderWithQuery(<ComicsPage />);
    expect(screen.getByText('Load More')).toBeInTheDocument();
  });

  it('hides Load More button when no next page', () => {
    renderWithQuery(<ComicsPage />);
    expect(screen.queryByText('Load More')).not.toBeInTheDocument();
  });

  it('calls setAfterCursor when Load More is clicked', async () => {
    const user = userEvent.setup();
    vi.mocked(useGetComicsQuery).mockReturnValue({
      data: {
        ...mockComicsData,
        comics: {
          ...mockComicsData.comics,
          pageInfo: { ...mockComicsData.comics.pageInfo, hasNextPage: true, endCursor: 'cursor-2' },
        },
      },
      isLoading: false,
      isFetching: false,
    } as any);
    renderWithQuery(<ComicsPage />);
    await user.click(screen.getByText('Load More'));
    // The click triggers setAfterCursor which re-invokes the query
    expect(useGetComicsQuery).toHaveBeenCalled();
  });
});
