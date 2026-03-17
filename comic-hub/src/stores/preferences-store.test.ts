import { describe, it, expect, beforeEach, vi } from 'vitest';
import { usePreferencesStore } from './preferences-store';
import { DEFAULT_DISPLAY_SETTINGS } from '@/lib/preferences-defaults';

describe('preferences-store', () => {
  let mockClassList: { add: ReturnType<typeof vi.fn>; remove: ReturnType<typeof vi.fn> };
  let mockSetAttribute: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    usePreferencesStore.setState({
      settings: DEFAULT_DISPLAY_SETTINGS,
      isHydrated: false,
    });

    mockClassList = { add: vi.fn(), remove: vi.fn() };
    mockSetAttribute = vi.fn();

    Object.defineProperty(document, 'documentElement', {
      value: { classList: mockClassList, setAttribute: mockSetAttribute },
      writable: true,
      configurable: true,
    });

    Object.defineProperty(window, 'matchMedia', {
      value: vi.fn().mockReturnValue({ matches: false }),
      writable: true,
      configurable: true,
    });
  });

  describe('hydrate', () => {
    it('merges server data with defaults', () => {
      const { hydrate } = usePreferencesStore.getState();

      hydrate({ theme: 'dark', comicsPerPage: 48 });

      const { settings, isHydrated } = usePreferencesStore.getState();
      expect(isHydrated).toBe(true);
      expect(settings.theme).toBe('dark');
      expect(settings.comicsPerPage).toBe(48);
      expect(settings.showContinueReading).toBe(true);
      expect(settings.defaultZoom).toBe(100);
    });

    it('uses all defaults when server data is null', () => {
      const { hydrate } = usePreferencesStore.getState();

      hydrate(null);

      const { settings } = usePreferencesStore.getState();
      expect(settings).toEqual(DEFAULT_DISPLAY_SETTINGS);
    });

    it('applies theme to DOM', () => {
      const { hydrate } = usePreferencesStore.getState();

      hydrate({ theme: 'dark' });

      expect(mockClassList.remove).toHaveBeenCalledWith('light', 'dark');
      expect(mockClassList.add).toHaveBeenCalledWith('dark');
      expect(mockSetAttribute).toHaveBeenCalledWith('data-theme', 'dark');
    });
  });

  describe('setSettings', () => {
    it('applies partial updates', () => {
      const { hydrate } = usePreferencesStore.getState();
      hydrate({});

      const { setSettings } = usePreferencesStore.getState();
      setSettings({ showFavorites: false, defaultZoom: 150 });

      const { settings } = usePreferencesStore.getState();
      expect(settings.showFavorites).toBe(false);
      expect(settings.defaultZoom).toBe(150);
      expect(settings.showContinueReading).toBe(true);
    });

    it('applies theme to DOM when theme changes', () => {
      const { hydrate } = usePreferencesStore.getState();
      hydrate({});
      mockClassList.remove.mockClear();
      mockClassList.add.mockClear();

      const { setSettings } = usePreferencesStore.getState();
      setSettings({ theme: 'light' });

      expect(mockClassList.add).toHaveBeenCalledWith('light');
    });

    it('does not apply theme to DOM when theme is unchanged', () => {
      const { hydrate } = usePreferencesStore.getState();
      hydrate({});
      mockClassList.remove.mockClear();
      mockClassList.add.mockClear();

      const { setSettings } = usePreferencesStore.getState();
      setSettings({ comicsPerPage: 96 });

      expect(mockClassList.remove).not.toHaveBeenCalled();
    });
  });
});
