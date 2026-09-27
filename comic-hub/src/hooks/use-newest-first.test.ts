import { renderHook, act } from '@testing-library/react';
import { useNewestFirst } from './use-newest-first';
import { usePreferencesStore } from '@/stores/preferences-store';
import { DEFAULT_DISPLAY_SETTINGS } from '@/lib/preferences-defaults';

describe('useNewestFirst', () => {
  afterEach(() => {
    usePreferencesStore.setState({ settings: DEFAULT_DISPLAY_SETTINGS });
  });

  it('is false for the default catch-up order', () => {
    const { result } = renderHook(() => useNewestFirst());
    expect(result.current).toBe(false);
  });

  it('follows the scroll order preference', () => {
    const { result } = renderHook(() => useNewestFirst());

    act(() => usePreferencesStore.getState().setSettings({ readerScrollOrder: 'newest-first' }));
    expect(result.current).toBe(true);
  });
});
