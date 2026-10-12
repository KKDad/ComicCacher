import { renderHook, act } from '@testing-library/react';
import { PHONE_LANDSCAPE_QUERY, usePhoneLandscape } from './use-phone-landscape';

describe('usePhoneLandscape', () => {
  it('follows the phone-landscape media query', () => {
    let matches = false;
    let listener: (() => void) | undefined;
    window.matchMedia = vi.fn().mockImplementation((query: string) => ({
      get matches() { return query === PHONE_LANDSCAPE_QUERY && matches; },
      media: query,
      addEventListener: (_: string, l: () => void) => { listener = l; },
      removeEventListener: vi.fn(),
    }));

    const { result } = renderHook(() => usePhoneLandscape());
    expect(result.current).toBe(false);

    act(() => {
      matches = true;
      listener?.();
    });
    expect(result.current).toBe(true);
  });
});
