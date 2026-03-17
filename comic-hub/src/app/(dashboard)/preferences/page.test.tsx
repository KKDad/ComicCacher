import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PreferencesPage from './page';
import {
  useGetUserPreferencesQuery,
  useUpdateDisplaySettingsMutation,
} from '@/generated/graphql';
import { usePreferencesStore } from '@/stores/preferences-store';
import { DEFAULT_DISPLAY_SETTINGS } from '@/lib/preferences-defaults';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

vi.mock('sonner', () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}));

vi.mock('@/generated/graphql', () => ({
  useGetUserPreferencesQuery: vi.fn(),
  useUpdateDisplaySettingsMutation: vi.fn(),
}));

const mockMutate = vi.fn();

function renderWithQuery(ui: React.ReactElement) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={qc}>{ui}</QueryClientProvider>);
}

describe('PreferencesPage', () => {
  beforeEach(() => {
    usePreferencesStore.setState({
      settings: DEFAULT_DISPLAY_SETTINGS,
      isHydrated: true,
    });

    vi.mocked(useGetUserPreferencesQuery).mockReturnValue({
      data: { preferences: { displaySettings: DEFAULT_DISPLAY_SETTINGS } },
      isLoading: false,
    } as any);

    vi.mocked(useUpdateDisplaySettingsMutation).mockReturnValue({
      mutate: mockMutate,
    } as any);

    mockMutate.mockClear();
  });

  it('renders all sections', () => {
    renderWithQuery(<PreferencesPage />);

    expect(screen.getByText('Appearance')).toBeInTheDocument();
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Reading')).toBeInTheDocument();
    expect(screen.getByText('Display')).toBeInTheDocument();
  });

  it('renders theme toggle buttons', () => {
    renderWithQuery(<PreferencesPage />);

    expect(screen.getByRole('button', { name: /light/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /dark/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /system/i })).toBeInTheDocument();
  });

  it('renders dashboard switches', () => {
    renderWithQuery(<PreferencesPage />);

    expect(screen.getByText('Continue Reading')).toBeInTheDocument();
    expect(screen.getByText('Favorites')).toBeInTheDocument();
    expect(screen.getByText('Recently Added')).toBeInTheDocument();
  });

  it('updates store when theme button is clicked', async () => {
    const user = userEvent.setup();
    renderWithQuery(<PreferencesPage />);

    await user.click(screen.getByRole('button', { name: /dark/i }));

    expect(usePreferencesStore.getState().settings.theme).toBe('dark');
  });

  it('fires mutation with full settings after debounce', async () => {
    const user = userEvent.setup();
    renderWithQuery(<PreferencesPage />);

    await user.click(screen.getByRole('button', { name: /dark/i }));

    await waitFor(() => {
      expect(mockMutate).toHaveBeenCalled();
    });

    const calledWith = mockMutate.mock.calls[0][0].settings;
    expect(calledWith).toHaveProperty('theme', 'dark');
    expect(calledWith).toHaveProperty('showContinueReading');
    expect(calledWith).toHaveProperty('showFavorites');
    expect(calledWith).toHaveProperty('showRecentlyAdded');
    expect(calledWith).toHaveProperty('readingDirection');
    expect(calledWith).toHaveProperty('comicsPerPage');
    expect(calledWith).toHaveProperty('defaultZoom');
  });

  it('shows loading state when not hydrated', () => {
    usePreferencesStore.setState({ isHydrated: false });

    vi.mocked(useGetUserPreferencesQuery).mockReturnValue({
      data: undefined,
      isLoading: true,
    } as any);

    renderWithQuery(<PreferencesPage />);

    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });
});
