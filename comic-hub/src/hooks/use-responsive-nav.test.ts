import { renderHook, act } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { createElement } from 'react';
import { useResponsiveNav } from './use-responsive-nav';

type Listener = () => void;

function mockViewport(width: number) {
  const listeners = new Set<Listener>();
  let current = width;
  window.matchMedia = vi.fn().mockImplementation((query: string) => {
    const min = Number(/min-width: (\d+)px/.exec(query)![1]);
    return {
      get matches() { return current >= min; },
      media: query,
      addEventListener: (_: string, l: Listener) => listeners.add(l),
      removeEventListener: (_: string, l: Listener) => listeners.delete(l),
    };
  });
  return {
    resize(next: number) {
      current = next;
      listeners.forEach((l) => l());
    },
    listenerCount: () => listeners.size,
  };
}

describe('useResponsiveNav', () => {
  it.each([
    [1280, 'desktop'],
    [1024, 'desktop'],
    [900, 'tablet'],
    [768, 'tablet'],
    [375, 'mobile'],
  ])('reports %ipx as %s', (width, expected) => {
    mockViewport(width);
    const { result } = renderHook(() => useResponsiveNav());
    expect(result.current.layout).toBe(expected);
  });

  it('updates when the viewport crosses a breakpoint', () => {
    const viewport = mockViewport(1280);
    const { result } = renderHook(() => useResponsiveNav());
    act(() => viewport.resize(500));
    expect(result.current.layout).toBe('mobile');
  });

  it('unsubscribes on unmount', () => {
    const viewport = mockViewport(1280);
    const { unmount } = renderHook(() => useResponsiveNav());
    expect(viewport.listenerCount()).toBeGreaterThan(0);
    unmount();
    expect(viewport.listenerCount()).toBe(0);
  });

  it('is null during server rendering, where the viewport is unknown', () => {
    mockViewport(375);
    function Probe() {
      return createElement('span', null, String(useResponsiveNav().layout));
    }
    expect(renderToString(createElement(Probe))).toContain('null');
  });
});
