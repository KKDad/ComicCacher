import { render, screen } from '@testing-library/react';
import ComicsPage from './page';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useInfiniteGetComicsQuery, useSearchComicsQuery } from '@/generated/graphql';

vi.mock('@/generated/graphql', () => ({
  useInfiniteGetComicsQuery: vi.fn(),
  useSearchComicsQuery: vi.fn(),
}));

const mockSearchParams = new Map<string, string>();
vi.mock('next/navigation', () => ({
  useSearchParams: () => ({
    get: (key: string) => mockSearchParams.get(key) ?? null,
  }),
}));

// Mock IntersectionObserver
const mockObserve = vi.fn();
const mockDisconnect = vi.fn();

class MockIntersectionObserver {
  constructor(callback: IntersectionObserverCallback) {
    (globalThis as any).__ioCallback = callback;
  }
  observe = mockObserve;
  disconnect = mockDisconnect;
  unobserve = vi.fn();
}

vi.stubGlobal('IntersectionObserver', MockIntersectionObserver);

function renderWithQuery(ui: React.ReactElement) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={qc}>{ui}</QueryClientProvider>);
}

const mockPage = {
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
    totalCount: 42,
  },
};

const mockFetchNextPage = vi.fn();

describe('ComicsPage', () => {
  beforeEach(() => {
    mockSearchParams.clear();
    vi.mocked(useInfiniteGetComicsQuery).mockReturnValue({
      data: { pages: [mockPage] },
      isLoading: false,
      isFetchingNextPage: false,
      hasNextPage: false,
      fetchNextPage: mockFetchNextPage,
    } as any);
    vi.mocked(useSearchComicsQuery).mockReturnValue({
      data: undefined,
      isLoading: false,
    } as any);
    mockFetchNextPage.mockClear();
    mockObserve.mockClear();
    mockDisconnect.mockClear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders loading skeletons when isLoading', () => {
    vi.mocked(useInfiniteGetComicsQuery).mockReturnValue({
      data: undefined,
      isLoading: true,
      isFetchingNextPage: false,
      hasNextPage: false,
      fetchNextPage: mockFetchNextPage,
    } as any);
    renderWithQuery(<ComicsPage />);
    expect(screen.queryByText('Browse Comics')).not.toBeInTheDocument();
  });

  it('renders empty state when no comics', () => {
    vi.mocked(useInfiniteGetComicsQuery).mockReturnValue({
      data: {
        pages: [{
          comics: {
            edges: [],
            pageInfo: { hasNextPage: false, hasPreviousPage: false, startCursor: null, endCursor: null },
            totalCount: 0,
          },
        }],
      },
      isLoading: false,
      isFetchingNextPage: false,
      hasNextPage: false,
      fetchNextPage: mockFetchNextPage,
    } as any);
    renderWithQuery(<ComicsPage />);
    expect(screen.getByText('No comics available')).toBeInTheDocument();
  });

  it('renders comic tiles', () => {
    renderWithQuery(<ComicsPage />);
    expect(screen.getByText('Garfield')).toBeInTheDocument();
    expect(screen.getByText('Peanuts')).toBeInTheDocument();
  });

  it('shows total count from server, not loaded count', () => {
    renderWithQuery(<ComicsPage />);
    expect(screen.getByText(/42 available comics/)).toBeInTheDocument();
  });

  it('sets up IntersectionObserver on sentinel', () => {
    renderWithQuery(<ComicsPage />);
    expect(mockObserve).toHaveBeenCalled();
  });

  it('calls fetchNextPage when sentinel is intersecting', () => {
    vi.mocked(useInfiniteGetComicsQuery).mockReturnValue({
      data: { pages: [mockPage] },
      isLoading: false,
      isFetchingNextPage: false,
      hasNextPage: true,
      fetchNextPage: mockFetchNextPage,
    } as any);

    renderWithQuery(<ComicsPage />);

    // Simulate intersection
    const callback = (globalThis as any).__ioCallback;
    callback([{ isIntersecting: true }]);

    expect(mockFetchNextPage).toHaveBeenCalled();
  });

  it('does not call fetchNextPage when not intersecting', () => {
    vi.mocked(useInfiniteGetComicsQuery).mockReturnValue({
      data: { pages: [mockPage] },
      isLoading: false,
      isFetchingNextPage: false,
      hasNextPage: true,
      fetchNextPage: mockFetchNextPage,
    } as any);

    renderWithQuery(<ComicsPage />);

    const callback = (globalThis as any).__ioCallback;
    callback([{ isIntersecting: false }]);

    expect(mockFetchNextPage).not.toHaveBeenCalled();
  });

  it('does not call fetchNextPage when already fetching', () => {
    vi.mocked(useInfiniteGetComicsQuery).mockReturnValue({
      data: { pages: [mockPage] },
      isLoading: false,
      isFetchingNextPage: true,
      hasNextPage: true,
      fetchNextPage: mockFetchNextPage,
    } as any);

    renderWithQuery(<ComicsPage />);

    const callback = (globalThis as any).__ioCallback;
    callback([{ isIntersecting: true }]);

    expect(mockFetchNextPage).not.toHaveBeenCalled();
  });

  it('shows spinner when fetching next page', () => {
    vi.mocked(useInfiniteGetComicsQuery).mockReturnValue({
      data: { pages: [mockPage] },
      isLoading: false,
      isFetchingNextPage: true,
      hasNextPage: true,
      fetchNextPage: mockFetchNextPage,
    } as any);

    renderWithQuery(<ComicsPage />);
    expect(document.querySelector('.animate-spin')).toBeInTheDocument();
  });

  it('flattens multiple pages of comics', () => {
    const page2 = {
      comics: {
        edges: [
          {
            node: {
              id: 3,
              name: 'Calvin and Hobbes',
              newest: '2024-01-15',
              avatarUrl: null,
              lastStrip: null,
            },
          },
        ],
        pageInfo: { hasNextPage: false, hasPreviousPage: true, startCursor: 'c3', endCursor: 'c3' },
        totalCount: 42,
      },
    };

    vi.mocked(useInfiniteGetComicsQuery).mockReturnValue({
      data: { pages: [mockPage, page2] },
      isLoading: false,
      isFetchingNextPage: false,
      hasNextPage: false,
      fetchNextPage: mockFetchNextPage,
    } as any);

    renderWithQuery(<ComicsPage />);
    expect(screen.getByText('Garfield')).toBeInTheDocument();
    expect(screen.getByText('Calvin and Hobbes')).toBeInTheDocument();
  });

  it('disconnects observer on unmount', () => {
    const { unmount } = renderWithQuery(<ComicsPage />);
    unmount();
    expect(mockDisconnect).toHaveBeenCalled();
  });
});

describe('ComicsPage - search mode', () => {
  beforeEach(() => {
    mockSearchParams.set('q', 'garfield');
    vi.mocked(useInfiniteGetComicsQuery).mockReturnValue({
      data: undefined,
      isLoading: false,
      isFetchingNextPage: false,
      hasNextPage: false,
      fetchNextPage: vi.fn(),
    } as any);
  });

  afterEach(() => {
    mockSearchParams.clear();
    vi.restoreAllMocks();
  });

  it('renders search results when query param is present', () => {
    vi.mocked(useSearchComicsQuery).mockReturnValue({
      data: {
        search: {
          comics: [
            { id: 1, name: 'Garfield', newest: '2024-01-15', avatarUrl: null, lastStrip: null },
          ],
        },
      },
      isLoading: false,
    } as any);

    renderWithQuery(<ComicsPage />);
    expect(screen.getByText('Search Results')).toBeInTheDocument();
    expect(screen.getByText(/1 comic matching "garfield"/)).toBeInTheDocument();
    expect(screen.getByText('Garfield')).toBeInTheDocument();
  });

  it('shows empty state when search has no results', () => {
    vi.mocked(useSearchComicsQuery).mockReturnValue({
      data: { search: { comics: [] } },
      isLoading: false,
    } as any);

    renderWithQuery(<ComicsPage />);
    expect(screen.getByText('No comics found')).toBeInTheDocument();
    expect(screen.getByText(/No comics matching "garfield"/)).toBeInTheDocument();
  });

  it('shows loading state during search', () => {
    vi.mocked(useSearchComicsQuery).mockReturnValue({
      data: undefined,
      isLoading: true,
    } as any);

    renderWithQuery(<ComicsPage />);
    expect(screen.queryByText('Search Results')).not.toBeInTheDocument();
  });

  it('pluralizes result count correctly', () => {
    vi.mocked(useSearchComicsQuery).mockReturnValue({
      data: {
        search: {
          comics: [
            { id: 1, name: 'Garfield', newest: '2024-01-15', avatarUrl: null, lastStrip: null },
            { id: 2, name: 'Garfield Minus Garfield', newest: '2024-01-15', avatarUrl: null, lastStrip: null },
          ],
        },
      },
      isLoading: false,
    } as any);

    renderWithQuery(<ComicsPage />);
    expect(screen.getByText(/2 comics matching "garfield"/)).toBeInTheDocument();
  });
});
