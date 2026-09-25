import { describe, it, expect, beforeEach } from 'vitest';
import { usePreferencesStore } from './preferences-store';
import { DEFAULT_DISPLAY_SETTINGS } from '@/lib/preferences-defaults';

describe('preferences-store', () => {
  beforeEach(() => {
    usePreferencesStore.setState({
      settings: DEFAULT_DISPLAY_SETTINGS,
      isHydrated: false,
    });

  });

  describe('hydrate', () => {
    it('merges server data with defaults', () => {
      const { hydrate } = usePreferencesStore.getState();

      hydrate({ theme: 'dark', comicsPerPage: 48 });

      const { settings, isHydrated } = usePreferencesStore.getState();
      expect(isHydrated).toBe(true);
      expect(settings.theme).toBe('dark');
      // Keys the UI no longer uses survive, so saving doesn't erase them server-side.
      expect((settings as unknown as Record<string, unknown>).comicsPerPage).toBe(48);
      expect(settings.showContinueReading).toBe(true);
    });

    it('uses all defaults when server data is null', () => {
      const { hydrate } = usePreferencesStore.getState();

      hydrate(null);

      const { settings } = usePreferencesStore.getState();
      expect(settings).toEqual(DEFAULT_DISPLAY_SETTINGS);
    });



  });

  describe('setSettings', () => {
    it('applies partial updates', () => {
      const { hydrate } = usePreferencesStore.getState();
      hydrate({});

      const { setSettings } = usePreferencesStore.getState();
      setSettings({ showFavorites: false, readerNavMode: 'all' });

      const { settings } = usePreferencesStore.getState();
      expect(settings.showFavorites).toBe(false);
      expect(settings.readerNavMode).toBe('all');
      expect(settings.showContinueReading).toBe(true);
    });


  });

  it('leaves the <html> theme class to next-themes', () => {
    document.documentElement.className = '';
    usePreferencesStore.getState().hydrate({ theme: 'dark' });
    usePreferencesStore.getState().setSettings({ theme: 'light' });
    expect(document.documentElement.className).toBe('');
    expect(usePreferencesStore.getState().settings.theme).toBe('light');
  });
});
