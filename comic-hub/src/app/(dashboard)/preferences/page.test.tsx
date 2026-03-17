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

  it('shows success toast on mutation success without errors', async () => {
    const { toast } = await import('sonner');
    let capturedOpts: any;
    vi.mocked(useUpdateDisplaySettingsMutation).mockImplementation((opts: any) => {
      capturedOpts = opts;
      return { mutate: mockMutate } as any;
    });

    renderWithQuery(<PreferencesPage />);

    capturedOpts.onSuccess({ updateDisplaySettings: { errors: [] } });
    expect(toast.success).toHaveBeenCalledWith('Preferences saved');
  });

  it('shows error toast on mutation success with errors', async () => {
    const { toast } = await import('sonner');
    let capturedOpts: any;
    vi.mocked(useUpdateDisplaySettingsMutation).mockImplementation((opts: any) => {
      capturedOpts = opts;
      return { mutate: mockMutate } as any;
    });

    renderWithQuery(<PreferencesPage />);

    capturedOpts.onSuccess({ updateDisplaySettings: { errors: [{ message: 'bad' }] } });
    expect(toast.error).toHaveBeenCalledWith('Failed to save preferences');
  });

  it('shows error toast on mutation error', async () => {
    const { toast } = await import('sonner');
    let capturedOpts: any;
    vi.mocked(useUpdateDisplaySettingsMutation).mockImplementation((opts: any) => {
      capturedOpts = opts;
      return { mutate: mockMutate } as any;
    });

    renderWithQuery(<PreferencesPage />);

    capturedOpts.onError(new Error('network'));
    expect(toast.error).toHaveBeenCalledWith('Failed to save preferences');
  });

  it('toggles dashboard switch and fires mutation', async () => {
    const user = userEvent.setup();
    renderWithQuery(<PreferencesPage />);

    // The switches are rendered as radix switches — click the first one (Continue Reading)
    const switches = screen.getAllByRole('switch');
    await user.click(switches[0]);

    expect(usePreferencesStore.getState().settings.showContinueReading).toBe(false);

    await waitFor(() => {
      expect(mockMutate).toHaveBeenCalledWith({
        settings: expect.objectContaining({ showContinueReading: false }),
      });
    });
  });

  it('toggles reading direction', async () => {
    const user = userEvent.setup();
    renderWithQuery(<PreferencesPage />);

    await user.click(screen.getByRole('button', { name: /oldest first/i }));

    expect(usePreferencesStore.getState().settings.readingDirection).toBe('oldest-first');

    await waitFor(() => {
      expect(mockMutate).toHaveBeenCalledWith({
        settings: expect.objectContaining({ readingDirection: 'oldest-first' }),
      });
    });
  });

  it('renders comics per page and zoom selects', () => {
    renderWithQuery(<PreferencesPage />);

    const comboboxes = screen.getAllByRole('combobox');
    expect(comboboxes).toHaveLength(2);
    expect(screen.getByText('Comics per page')).toBeInTheDocument();
    expect(screen.getByText('Default zoom')).toBeInTheDocument();
  });

  it('toggles favorites switch', async () => {
    const user = userEvent.setup();
    renderWithQuery(<PreferencesPage />);

    const switches = screen.getAllByRole('switch');
    await user.click(switches[1]); // Favorites is the second switch

    expect(usePreferencesStore.getState().settings.showFavorites).toBe(false);
  });

  it('toggles recently added switch', async () => {
    const user = userEvent.setup();
    renderWithQuery(<PreferencesPage />);

    const switches = screen.getAllByRole('switch');
    await user.click(switches[2]); // Recently Added is the third switch

    expect(usePreferencesStore.getState().settings.showRecentlyAdded).toBe(false);
  });

  it('selects newest-first reading direction', async () => {
    usePreferencesStore.setState({
      settings: { ...DEFAULT_DISPLAY_SETTINGS, readingDirection: 'oldest-first' },
      isHydrated: true,
    });

    const user = userEvent.setup();
    renderWithQuery(<PreferencesPage />);

    await user.click(screen.getByRole('button', { name: /newest first/i }));

    expect(usePreferencesStore.getState().settings.readingDirection).toBe('newest-first');
  });


});
