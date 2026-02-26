import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useThemeStore } from './theme-store';

describe('theme-store', () => {
  let mockClassList: {
    add: ReturnType<typeof vi.fn>;
    remove: ReturnType<typeof vi.fn>;
  };
  let mockSetAttribute: ReturnType<typeof vi.fn>;
  let mockMatchMedia: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    // Reset store to initial state
    useThemeStore.setState({ theme: 'system' });

    // Mock document.documentElement.classList
    mockClassList = {
      add: vi.fn(),
      remove: vi.fn(),
    };

    mockSetAttribute = vi.fn();

    Object.defineProperty(document, 'documentElement', {
      value: {
        classList: mockClassList,
        setAttribute: mockSetAttribute,
      },
      writable: true,
      configurable: true,
    });

    // Mock window.matchMedia
    mockMatchMedia = vi.fn();
    Object.defineProperty(window, 'matchMedia', {
      value: mockMatchMedia,
      writable: true,
      configurable: true,
    });
  });

  describe('initial state', () => {
    it('starts with theme as system', () => {
      const { theme } = useThemeStore.getState();
      expect(theme).toBe('system');
    });
  });

  describe('setTheme', () => {
    it('sets theme to light and applies light class', () => {
      mockMatchMedia.mockReturnValue({ matches: false });
      const { setTheme } = useThemeStore.getState();

      setTheme('light');

      expect(useThemeStore.getState().theme).toBe('light');
      expect(mockClassList.remove).toHaveBeenCalledWith('light', 'dark');
      expect(mockClassList.add).toHaveBeenCalledWith('light');
      expect(mockSetAttribute).toHaveBeenCalledWith('data-theme', 'light');
    });

    it('sets theme to dark and applies dark class', () => {
      mockMatchMedia.mockReturnValue({ matches: true });
      const { setTheme } = useThemeStore.getState();

      setTheme('dark');

      expect(useThemeStore.getState().theme).toBe('dark');
      expect(mockClassList.remove).toHaveBeenCalledWith('light', 'dark');
      expect(mockClassList.add).toHaveBeenCalledWith('dark');
      expect(mockSetAttribute).toHaveBeenCalledWith('data-theme', 'dark');
    });

    it('sets theme to system and applies dark class when system prefers dark', () => {
      mockMatchMedia.mockReturnValue({ matches: true });
      const { setTheme } = useThemeStore.getState();

      setTheme('system');

      expect(useThemeStore.getState().theme).toBe('system');
      expect(mockClassList.remove).toHaveBeenCalledWith('light', 'dark');
      expect(mockMatchMedia).toHaveBeenCalledWith('(prefers-color-scheme: dark)');
      expect(mockClassList.add).toHaveBeenCalledWith('dark');
      expect(mockSetAttribute).toHaveBeenCalledWith('data-theme', 'dark');
    });

    it('sets theme to system and applies light class when system prefers light', () => {
      mockMatchMedia.mockReturnValue({ matches: false });
      const { setTheme } = useThemeStore.getState();

      setTheme('system');

      expect(useThemeStore.getState().theme).toBe('system');
      expect(mockClassList.remove).toHaveBeenCalledWith('light', 'dark');
      expect(mockMatchMedia).toHaveBeenCalledWith('(prefers-color-scheme: dark)');
      expect(mockClassList.add).toHaveBeenCalledWith('light');
      expect(mockSetAttribute).toHaveBeenCalledWith('data-theme', 'light');
    });

    it('removes existing theme classes before applying new theme', () => {
      mockMatchMedia.mockReturnValue({ matches: false });
      const { setTheme } = useThemeStore.getState();

      setTheme('dark');
      mockClassList.remove.mockClear();
      setTheme('light');

      expect(mockClassList.remove).toHaveBeenCalledWith('light', 'dark');
    });
  });
});
