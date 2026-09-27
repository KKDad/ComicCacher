import { render, act } from '@testing-library/react';
import { ThemeSync } from './theme-sync';
import { usePreferencesStore } from '@/stores/preferences-store';
import { DEFAULT_DISPLAY_SETTINGS } from '@/lib/preferences-defaults';

function mockSystemDark(matches: boolean) {
  const listeners = new Set<() => void>();
  const query = {
    matches,
    addEventListener: vi.fn((_: string, cb: () => void) => listeners.add(cb)),
    removeEventListener: vi.fn((_: string, cb: () => void) => listeners.delete(cb)),
  };
  Object.defineProperty(window, 'matchMedia', {
    value: vi.fn().mockReturnValue(query),
    writable: true,
    configurable: true,
  });
  return {
    query,
    setMatches: (next: boolean) => {
      query.matches = next;
      listeners.forEach((cb) => cb());
    },
  };
}

const theme = () => document.documentElement.getAttribute('data-theme');

describe('ThemeSync', () => {
  beforeEach(() => {
    document.documentElement.classList.remove('light', 'dark');
    document.documentElement.removeAttribute('data-theme');
    usePreferencesStore.setState({ settings: DEFAULT_DISPLAY_SETTINGS, isHydrated: false });
  });

  it('applies the stored theme on mount', () => {
    mockSystemDark(false);
    usePreferencesStore.setState({ settings: { ...DEFAULT_DISPLAY_SETTINGS, theme: 'dark' } });

    render(<ThemeSync />);

    expect(theme()).toBe('dark');
  });

  it('follows theme changes in the store', () => {
    mockSystemDark(false);
    render(<ThemeSync />);

    act(() => usePreferencesStore.getState().setSettings({ theme: 'dark' }));
    expect(theme()).toBe('dark');

    act(() => usePreferencesStore.getState().setSettings({ theme: 'light' }));
    expect(theme()).toBe('light');
  });

  it('follows the OS while the preference is system', () => {
    const system = mockSystemDark(false);
    render(<ThemeSync />);
    expect(theme()).toBe('light');

    act(() => system.setMatches(true));
    expect(theme()).toBe('dark');
  });

  it('stops following the OS once an explicit theme is chosen', () => {
    const system = mockSystemDark(false);
    render(<ThemeSync />);

    act(() => usePreferencesStore.getState().setSettings({ theme: 'light' }));
    expect(system.query.removeEventListener).toHaveBeenCalled();

    act(() => system.setMatches(true));
    expect(theme()).toBe('light');
  });
});
