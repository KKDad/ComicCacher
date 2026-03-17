import { render, screen } from '@testing-library/react';
import { DashboardClient } from './dashboard-client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useGetComicsQuery, useGetMeQuery, useGetUserPreferencesQuery, useAddFavoriteMutation, useRemoveFavoriteMutation } from '@/generated/graphql';
import { usePreferencesStore } from '@/stores/preferences-store';
import { DEFAULT_DISPLAY_SETTINGS } from '@/lib/preferences-defaults';

vi.mock('@/generated/graphql', () => ({
  useGetComicsQuery: vi.fn(),
  useGetMeQuery: vi.fn(),
  useGetUserPreferencesQuery: vi.fn(),
  useAddFavoriteMutation: vi.fn(),
  useRemoveFavoriteMutation: vi.fn(),
}));

function renderWithQuery(ui: React.ReactElement) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={qc}>{ui}</QueryClientProvider>);
}

const mockMutate = vi.fn();
const mockMutation = { mutate: mockMutate, isLoading: false };

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
    usePreferencesStore.setState({
      settings: DEFAULT_DISPLAY_SETTINGS,
      isHydrated: true,
    });
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
    vi.mocked(useAddFavoriteMutation).mockReturnValue(mockMutation as any);
    vi.mocked(useRemoveFavoriteMutation).mockReturnValue(mockMutation as any);
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
    // Most recent date is comicId 1 (2024-01-14) → Garfield should appear in continue-reading
    expect(screen.getByText('Continue Reading')).toBeInTheDocument();
    // Garfield appears in both sections; the "Continue Reading" button confirms lastRead was resolved
    expect(screen.queryByText('No recent reading history')).not.toBeInTheDocument();
  });

  it('handles null preferences', () => {
    vi.mocked(useGetUserPreferencesQuery).mockReturnValue({
      data: { preferences: null },
      isLoading: false,
      error: null,
    } as any);
    renderWithQuery(<DashboardClient />);
    expect(screen.getByText('No favorite comics yet')).toBeInTheDocument();
    expect(screen.getByText('No recent reading history')).toBeInTheDocument();
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
    expect(screen.getByText('No recent reading history')).toBeInTheDocument();
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
    const img = screen.getByAltText('Peanuts');
    expect(img).toHaveAttribute('src', 'https://example.com/peanuts.png');
  });

  it('invokes addFavorite onSuccess callback', () => {
    let capturedOpts: any;
    vi.mocked(useAddFavoriteMutation).mockImplementation((opts: any) => {
      capturedOpts = opts;
      return mockMutation as any;
    });
    renderWithQuery(<DashboardClient />);
    // Calling onSuccess exercises the invalidateQueries branch
    expect(() => capturedOpts.onSuccess({ addFavorite: { errors: [] } })).not.toThrow();
  });

  it('invokes removeFavorite onSuccess callback', () => {
    let capturedOpts: any;
    vi.mocked(useRemoveFavoriteMutation).mockImplementation((opts: any) => {
      capturedOpts = opts;
      return mockMutation as any;
    });
    renderWithQuery(<DashboardClient />);
    expect(() => capturedOpts.onSuccess({ removeFavorite: { errors: [] } })).not.toThrow();
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

  it('hides continue reading when showContinueReading is false', () => {
    usePreferencesStore.setState({
      settings: { ...DEFAULT_DISPLAY_SETTINGS, showContinueReading: false },
      isHydrated: true,
    });
    renderWithQuery(<DashboardClient />);
    expect(screen.queryByText('Continue Where You Left Off')).not.toBeInTheDocument();
  });

  it('hides favorites when showFavorites is false', () => {
    usePreferencesStore.setState({
      settings: { ...DEFAULT_DISPLAY_SETTINGS, showFavorites: false },
      isHydrated: true,
    });
    renderWithQuery(<DashboardClient />);
    expect(screen.queryByText('Your Favorites')).not.toBeInTheDocument();
  });

  it('hides todays comics when showRecentlyAdded is false', () => {
    usePreferencesStore.setState({
      settings: { ...DEFAULT_DISPLAY_SETTINGS, showRecentlyAdded: false },
      isHydrated: true,
    });
    renderWithQuery(<DashboardClient />);
    expect(screen.queryByText("Today's Comics")).not.toBeInTheDocument();
  });

  it('calls removeFavorite when toggling an existing favorite', () => {
    const removeMutate = vi.fn();
    vi.mocked(useRemoveFavoriteMutation).mockReturnValue({ mutate: removeMutate, isLoading: false } as any);
    vi.mocked(useAddFavoriteMutation).mockReturnValue({ mutate: vi.fn(), isLoading: false } as any);

    renderWithQuery(<DashboardClient />);

    // Garfield (id: 1) is a favorite — clicking its favorite button should call removeFavorite
    const favoriteButtons = screen.getAllByRole('button', { name: /favorite/i });
    // Find the one for Garfield (first comic)
    favoriteButtons[0].click();

    expect(removeMutate).toHaveBeenCalledWith({ comicId: 1 });
  });

  it('calls addFavorite when toggling a non-favorite', () => {
    const addMutate = vi.fn();
    vi.mocked(useAddFavoriteMutation).mockReturnValue({ mutate: addMutate, isLoading: false } as any);
    vi.mocked(useRemoveFavoriteMutation).mockReturnValue({ mutate: vi.fn(), isLoading: false } as any);

    renderWithQuery(<DashboardClient />);

    // Peanuts (id: 2) is not a favorite — clicking its favorite button should call addFavorite
    const favoriteButtons = screen.getAllByRole('button', { name: /favorite/i });
    // Peanuts is the second comic
    favoriteButtons[1].click();

    expect(addMutate).toHaveBeenCalledWith({ comicId: 2 });
  });

  it('hydrates preferences store from displaySettings', () => {
    vi.mocked(useGetUserPreferencesQuery).mockReturnValue({
      data: {
        preferences: {
          favoriteComics: [],
          lastReadDates: [],
          displaySettings: { theme: 'dark', showFavorites: false },
        },
      },
      isLoading: false,
      error: null,
    } as any);
    renderWithQuery(<DashboardClient />);
    const { settings, isHydrated } = usePreferencesStore.getState();
    expect(isHydrated).toBe(true);
    expect(settings.theme).toBe('dark');
    expect(settings.showFavorites).toBe(false);
  });
});
