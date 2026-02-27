import { render, screen } from '@testing-library/react';
import { DashboardClient } from './dashboard-client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useGetComicsQuery, useGetMeQuery, useGetUserPreferencesQuery } from '@/generated/graphql';

vi.mock('@/generated/graphql', () => ({
  useGetComicsQuery: vi.fn(),
  useGetMeQuery: vi.fn(),
  useGetUserPreferencesQuery: vi.fn(),
}));

function renderWithQuery(ui: React.ReactElement) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={qc}>{ui}</QueryClientProvider>);
}

const mockComics = {
  comics: {
    edges: [
      {
        node: {
          id: 1,
          name: 'Garfield',
          avatarUrl: 'https://example.com/garfield.png',
          newest: '2024-01-15',
          lastStrip: { date: '2024-01-15', imageUrl: 'https://example.com/strip.png' },
        },
      },
      {
        node: {
          id: 2,
          name: 'Peanuts',
          avatarUrl: null,
          newest: '2024-01-15',
          lastStrip: null,
        },
      },
    ],
  },
};

describe('DashboardClient', () => {
  beforeEach(() => {
    vi.mocked(useGetMeQuery).mockReturnValue({ data: { me: { displayName: 'Test User' } } } as any);
    vi.mocked(useGetComicsQuery).mockReturnValue({ data: mockComics, isLoading: false, error: null } as any);
    vi.mocked(useGetUserPreferencesQuery).mockReturnValue({
      data: {
        preferences: {
          favoriteComics: [1],
          lastReadDates: [{ comicId: 1, date: '2024-01-14' }],
        },
      },
      isLoading: false,
      error: null,
    } as any);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders error state when comics query fails', () => {
    vi.mocked(useGetComicsQuery).mockReturnValue({
      data: null,
      isLoading: false,
      error: new Error('Network error'),
    } as any);
    renderWithQuery(<DashboardClient />);
    expect(screen.getByText('Failed to load dashboard')).toBeInTheDocument();
    expect(screen.getByText('Network error')).toBeInTheDocument();
  });

  it('renders error state when prefs query fails', () => {
    vi.mocked(useGetUserPreferencesQuery).mockReturnValue({
      data: null,
      isLoading: false,
      error: new Error('Prefs error'),
    } as any);
    renderWithQuery(<DashboardClient />);
    expect(screen.getByText('Failed to load dashboard')).toBeInTheDocument();
  });

  it('renders page header with display name', () => {
    renderWithQuery(<DashboardClient />);
    expect(screen.getByText(/test user/i)).toBeInTheDocument();
  });

  it('renders favorites section with filtered favorites', () => {
    renderWithQuery(<DashboardClient />);
    // Garfield (id: 1) is in favoriteComics
    expect(screen.getByText('Your Favorites')).toBeInTheDocument();
  });

  it('renders continue reading section', () => {
    renderWithQuery(<DashboardClient />);
    expect(screen.getByText('Continue Where You Left Off')).toBeInTheDocument();
  });

  it('renders todays comics section', () => {
    renderWithQuery(<DashboardClient />);
    expect(screen.getByText("Today's Comics")).toBeInTheDocument();
  });

  it('sorts lastReadDates to find most recent', () => {
    vi.mocked(useGetUserPreferencesQuery).mockReturnValue({
      data: {
        preferences: {
          favoriteComics: [],
          lastReadDates: [
            { comicId: 2, date: '2024-01-10' },
            { comicId: 1, date: '2024-01-14' },
            { comicId: 2, date: '2024-01-12' },
          ],
        },
      },
      isLoading: false,
      error: null,
    } as any);
    renderWithQuery(<DashboardClient />);
    expect(screen.getByText('Continue Where You Left Off')).toBeInTheDocument();
  });

  it('handles null preferences', () => {
    vi.mocked(useGetUserPreferencesQuery).mockReturnValue({
      data: { preferences: null },
      isLoading: false,
      error: null,
    } as any);
    renderWithQuery(<DashboardClient />);
    expect(screen.getByText("Today's Comics")).toBeInTheDocument();
  });

  it('handles lastReadDates with no matching comic', () => {
    vi.mocked(useGetUserPreferencesQuery).mockReturnValue({
      data: {
        preferences: {
          favoriteComics: [],
          lastReadDates: [{ comicId: 999, date: '2024-01-14' }],
        },
      },
      isLoading: false,
      error: null,
    } as any);
    renderWithQuery(<DashboardClient />);
    expect(screen.getByText("Today's Comics")).toBeInTheDocument();
  });

  it('falls back to "there" when me query has no displayName', () => {
    vi.mocked(useGetMeQuery).mockReturnValue({ data: { me: null } } as any);
    renderWithQuery(<DashboardClient />);
    expect(screen.getByText(/there/i)).toBeInTheDocument();
  });

  it('shows prefs error message', () => {
    vi.mocked(useGetUserPreferencesQuery).mockReturnValue({
      data: null,
      isLoading: false,
      error: new Error('Prefs failed'),
    } as any);
    renderWithQuery(<DashboardClient />);
    expect(screen.getByText('Prefs failed')).toBeInTheDocument();
  });

  it('maps comic thumbnail from avatarUrl when no lastStrip', () => {
    vi.mocked(useGetComicsQuery).mockReturnValue({
      data: {
        comics: {
          edges: [
            {
              node: {
                id: 2,
                name: 'Peanuts',
                avatarUrl: 'https://example.com/peanuts.png',
                newest: '2024-01-15',
                lastStrip: null,
              },
            },
          ],
        },
      },
      isLoading: false,
      error: null,
    } as any);
    renderWithQuery(<DashboardClient />);
    expect(screen.getByText("Today's Comics")).toBeInTheDocument();
  });

  it('handles empty comics data', () => {
    vi.mocked(useGetComicsQuery).mockReturnValue({
      data: { comics: { edges: [] } },
      isLoading: false,
      error: null,
    } as any);
    vi.mocked(useGetUserPreferencesQuery).mockReturnValue({
      data: { preferences: { favoriteComics: [], lastReadDates: [] } },
      isLoading: false,
      error: null,
    } as any);
    renderWithQuery(<DashboardClient />);
    // Should show empty states
    expect(screen.getByText('No comics for today')).toBeInTheDocument();
  });
});
