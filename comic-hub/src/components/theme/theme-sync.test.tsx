import { render } from '@testing-library/react';
import { ThemeSync } from './theme-sync';
import { usePreferencesStore } from '@/stores/preferences-store';
import { DEFAULT_DISPLAY_SETTINGS } from '@/lib/preferences-defaults';
import { useTheme } from 'next-themes';

vi.mock('next-themes', () => ({
  useTheme: vi.fn(),
}));

describe('ThemeSync', () => {
  const setTheme = vi.fn();

  beforeEach(() => {
    setTheme.mockClear();
    vi.mocked(useTheme).mockReturnValue({ setTheme } as any);
  });

  it('does nothing until preferences are known', () => {
    usePreferencesStore.setState({ settings: { ...DEFAULT_DISPLAY_SETTINGS, theme: 'dark' }, isHydrated: false });
    render(<ThemeSync />);
    expect(setTheme).not.toHaveBeenCalled();
  });

  it('applies the saved theme once hydrated', () => {
    usePreferencesStore.setState({ settings: { ...DEFAULT_DISPLAY_SETTINGS, theme: 'dark' }, isHydrated: true });
    render(<ThemeSync />);
    expect(setTheme).toHaveBeenCalledWith('dark');
  });

  it('follows later preference changes', () => {
    usePreferencesStore.setState({ settings: { ...DEFAULT_DISPLAY_SETTINGS, theme: 'dark' }, isHydrated: true });
    const { rerender } = render(<ThemeSync />);
    usePreferencesStore.getState().setSettings({ theme: 'system' });
    rerender(<ThemeSync />);
    expect(setTheme).toHaveBeenLastCalledWith('system');
  });
});
