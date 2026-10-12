import { renderHook, act, waitFor } from '@testing-library/react';
import { useFullscreen } from './use-fullscreen';

let fullscreenElement: Element | null = null;
const requestFullscreen = vi.fn();
const exitFullscreen = vi.fn();

function setFullscreen(element: Element | null) {
  fullscreenElement = element;
  document.dispatchEvent(new Event('fullscreenchange'));
}

beforeAll(() => {
  Object.defineProperty(document, 'fullscreenElement', { configurable: true, get: () => fullscreenElement });
  Object.defineProperty(document, 'exitFullscreen', { configurable: true, value: exitFullscreen });
});

beforeEach(() => {
  fullscreenElement = null;
  requestFullscreen.mockReset().mockResolvedValue(undefined);
  exitFullscreen.mockReset().mockResolvedValue(undefined);
  Object.defineProperty(document, 'fullscreenEnabled', { configurable: true, value: true });
  document.documentElement.requestFullscreen = requestFullscreen;
});

describe('useFullscreen', () => {
  it('is unsupported where the browser has no Fullscreen API', () => {
    Object.defineProperty(document, 'fullscreenEnabled', { configurable: true, value: undefined });
    const { result } = renderHook(() => useFullscreen());
    expect(result.current.supported).toBe(false);
  });

  it('enters and leaves full screen for the whole page', () => {
    const { result } = renderHook(() => useFullscreen());
    expect(result.current.supported).toBe(true);

    act(() => result.current.toggle());
    expect(requestFullscreen).toHaveBeenCalledWith({ navigationUI: 'hide' });

    act(() => setFullscreen(document.documentElement));
    expect(result.current.isFullscreen).toBe(true);

    act(() => result.current.toggle());
    expect(exitFullscreen).toHaveBeenCalled();

    act(() => setFullscreen(null));
    expect(result.current.isFullscreen).toBe(false);
  });

  it('reports a refused request instead of throwing', async () => {
    requestFullscreen.mockRejectedValue(new TypeError('Permissions check failed'));
    const { result } = renderHook(() => useFullscreen());

    await expect(result.current.enter()).resolves.toBe(false);
    act(() => result.current.toggle());
    await waitFor(() => expect(requestFullscreen).toHaveBeenCalledTimes(2));
  });

  it('leaves full screen when the component unmounts', () => {
    const { unmount } = renderHook(() => useFullscreen());
    act(() => setFullscreen(document.documentElement));

    unmount();

    expect(exitFullscreen).toHaveBeenCalled();
  });

  it('does nothing on unmount when not full screen', () => {
    const { unmount } = renderHook(() => useFullscreen());
    unmount();
    expect(exitFullscreen).not.toHaveBeenCalled();
  });
});
