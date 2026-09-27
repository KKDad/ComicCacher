import { describe, it, expect, beforeEach, vi } from 'vitest';
import { applyTheme, resolveTheme, watchSystemTheme, THEME_BOOTSTRAP_SCRIPT, PREFERENCES_STORAGE_KEY } from './theme';

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
  return { query, fire: () => listeners.forEach((cb) => cb()) };
}

function resetRoot() {
  const root = document.documentElement;
  root.classList.remove('light', 'dark');
  root.removeAttribute('data-theme');
}

describe('theme', () => {
  beforeEach(() => {
    localStorage.clear();
    resetRoot();
    mockSystemDark(false);
  });

  describe('resolveTheme', () => {
    it('returns explicit themes as-is', () => {
      expect(resolveTheme('light')).toBe('light');
      expect(resolveTheme('dark')).toBe('dark');
    });

    it('follows the OS for system', () => {
      mockSystemDark(true);
      expect(resolveTheme('system')).toBe('dark');
      mockSystemDark(false);
      expect(resolveTheme('system')).toBe('light');
    });
  });

  describe('applyTheme', () => {
    it('sets exactly one theme class and data-theme on <html>', () => {
      applyTheme('dark');
      applyTheme('light');

      const root = document.documentElement;
      expect(root.classList.contains('light')).toBe(true);
      expect(root.classList.contains('dark')).toBe(false);
      expect(root.getAttribute('data-theme')).toBe('light');
    });

    it('resolves system through the OS preference', () => {
      mockSystemDark(true);
      applyTheme('system');
      expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    });
  });

  describe('watchSystemTheme', () => {
    it('calls back on OS changes until unsubscribed', () => {
      const { query, fire } = mockSystemDark(false);
      const onChange = vi.fn();

      const stop = watchSystemTheme(onChange);
      fire();
      expect(onChange).toHaveBeenCalledTimes(1);

      stop();
      expect(query.removeEventListener).toHaveBeenCalledWith('change', onChange);
    });
  });

  describe('THEME_BOOTSTRAP_SCRIPT', () => {
    const run = () => new Function(THEME_BOOTSTRAP_SCRIPT)();

    it('applies the persisted theme', () => {
      localStorage.setItem(PREFERENCES_STORAGE_KEY, JSON.stringify({ state: { settings: { theme: 'dark' } }, version: 0 }));
      run();
      expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
      expect(document.documentElement.classList.contains('dark')).toBe(true);
    });

    it('falls back to the OS preference with nothing stored', () => {
      mockSystemDark(true);
      run();
      expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    });

    it('resolves a stored system theme through the OS preference', () => {
      localStorage.setItem(PREFERENCES_STORAGE_KEY, JSON.stringify({ state: { settings: { theme: 'system' } } }));
      run();
      expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    });

    it('ignores unreadable storage', () => {
      localStorage.setItem(PREFERENCES_STORAGE_KEY, '{not json');
      expect(run).not.toThrow();
    });
  });
});
