import { render } from '@testing-library/react';
import { PreferencesSync } from './preferences-sync';
import { useGetUserPreferencesQuery } from '@/generated/graphql';
import { usePreferencesStore } from '@/stores/preferences-store';
import { DEFAULT_DISPLAY_SETTINGS } from '@/lib/preferences-defaults';

vi.mock('@/generated/graphql', () => ({
  useGetUserPreferencesQuery: vi.fn(),
}));

type QueryResult = ReturnType<typeof useGetUserPreferencesQuery>;

function mockPreferences(data: unknown) {
  vi.mocked(useGetUserPreferencesQuery).mockReturnValue({ data } as unknown as QueryResult);
}

describe('PreferencesSync', () => {
  beforeEach(() => {
    usePreferencesStore.setState({ settings: DEFAULT_DISPLAY_SETTINGS, isHydrated: false });
  });

  it('loads saved display settings into the store', () => {
    mockPreferences({ preferences: { displaySettings: { theme: 'dark', showFavorites: false } } });

    render(<PreferencesSync />);

    const { settings, isHydrated } = usePreferencesStore.getState();
    expect(isHydrated).toBe(true);
    expect(settings.theme).toBe('dark');
    expect(settings.showFavorites).toBe(false);
  });

  it('waits for the query before hydrating', () => {
    mockPreferences(undefined);

    render(<PreferencesSync />);

    expect(usePreferencesStore.getState().isHydrated).toBe(false);
  });

  it('hydrates with defaults when the user has no saved settings', () => {
    mockPreferences({ preferences: { displaySettings: null } });

    render(<PreferencesSync />);

    const { settings, isHydrated } = usePreferencesStore.getState();
    expect(isHydrated).toBe(true);
    expect(settings).toEqual(DEFAULT_DISPLAY_SETTINGS);
  });
});
