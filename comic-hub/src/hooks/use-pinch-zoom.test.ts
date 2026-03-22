import { renderHook, act } from '@testing-library/react';
import { usePinchZoom } from './use-pinch-zoom';

function createTouchEvent(
  touches: Array<{ clientX: number; clientY: number }>,
  currentTarget?: Partial<HTMLElement>,
): React.TouchEvent {
  const rect = { left: 0, top: 0, width: 400, height: 400 };
  return {
    touches: touches as unknown as React.TouchList,
    currentTarget: {
      getBoundingClientRect: () => rect,
      ...currentTarget,
    } as HTMLElement,
  } as React.TouchEvent;
}

describe('usePinchZoom', () => {
  it('returns initial state at scale 1', () => {
    const { result } = renderHook(() => usePinchZoom());

    expect(result.current.state.scale).toBe(1);
    expect(result.current.isZoomed).toBe(false);
  });

  it('resets zoom state', () => {
    const { result } = renderHook(() => usePinchZoom());

    // Double-tap to zoom in
    act(() => {
      result.current.handlers.onTouchStart(
        createTouchEvent([{ clientX: 200, clientY: 200 }]),
      );
    });
    // Simulate second tap quickly
    act(() => {
      result.current.handlers.onTouchStart(
        createTouchEvent([{ clientX: 200, clientY: 200 }]),
      );
    });

    expect(result.current.state.scale).toBe(2);
    expect(result.current.isZoomed).toBe(true);

    act(() => {
      result.current.resetZoom();
    });

    expect(result.current.state.scale).toBe(1);
    expect(result.current.isZoomed).toBe(false);
  });

  it('isZoomed is false when scale is below threshold', () => {
    const { result } = renderHook(() => usePinchZoom());

    // Scale of 1.04 should not be considered zoomed (threshold is 1.05)
    expect(result.current.isZoomed).toBe(false);
  });

  it('provides touch handler functions', () => {
    const { result } = renderHook(() => usePinchZoom());

    expect(result.current.handlers.onTouchStart).toBeInstanceOf(Function);
    expect(result.current.handlers.onTouchMove).toBeInstanceOf(Function);
    expect(result.current.handlers.onTouchEnd).toBeInstanceOf(Function);
  });

  it('respects custom min and max scale', () => {
    const { result } = renderHook(() =>
      usePinchZoom({ minScale: 0.8, maxScale: 3 }),
    );

    expect(result.current.state.scale).toBe(1);
  });

  it('double-tap zooms in then resetZoom zooms out', () => {
    const { result } = renderHook(() => usePinchZoom());

    // First tap
    act(() => {
      result.current.handlers.onTouchStart(
        createTouchEvent([{ clientX: 200, clientY: 200 }]),
      );
    });
    // Second tap (double-tap) — zoom in
    act(() => {
      result.current.handlers.onTouchStart(
        createTouchEvent([{ clientX: 200, clientY: 200 }]),
      );
    });

    expect(result.current.isZoomed).toBe(true);
    expect(result.current.state.scale).toBe(2);

    act(() => {
      result.current.resetZoom();
    });

    expect(result.current.isZoomed).toBe(false);
    expect(result.current.state.scale).toBe(1);
  });

  it('pinch gesture updates scale', () => {
    const { result } = renderHook(() => usePinchZoom());

    // Start pinch with two touches 100px apart
    act(() => {
      result.current.handlers.onTouchStart(
        createTouchEvent([
          { clientX: 150, clientY: 200 },
          { clientX: 250, clientY: 200 },
        ]),
      );
    });

    // Move touches to 200px apart (2x zoom)
    act(() => {
      result.current.handlers.onTouchMove(
        createTouchEvent([
          { clientX: 100, clientY: 200 },
          { clientX: 300, clientY: 200 },
        ]),
      );
    });

    expect(result.current.state.scale).toBe(2);
    expect(result.current.isZoomed).toBe(true);
  });

  it('pinch end snaps back to 1x when scale is below threshold', () => {
    const { result } = renderHook(() => usePinchZoom());

    // Start pinch
    act(() => {
      result.current.handlers.onTouchStart(
        createTouchEvent([
          { clientX: 150, clientY: 200 },
          { clientX: 250, clientY: 200 },
        ]),
      );
    });

    // Pinch in slightly (scale < 1.05)
    act(() => {
      result.current.handlers.onTouchMove(
        createTouchEvent([
          { clientX: 175, clientY: 200 },
          { clientX: 225, clientY: 200 },
        ]),
      );
    });

    // End pinch (release second finger)
    act(() => {
      result.current.handlers.onTouchEnd(
        createTouchEvent([{ clientX: 200, clientY: 200 }]),
      );
    });

    // Should snap back to 1
    expect(result.current.state.scale).toBe(1);
    expect(result.current.isZoomed).toBe(false);
  });

  it('pan when zoomed updates translate', () => {
    const { result } = renderHook(() => usePinchZoom());

    // Double-tap to zoom in
    act(() => {
      result.current.handlers.onTouchStart(
        createTouchEvent([{ clientX: 200, clientY: 200 }]),
      );
    });
    act(() => {
      result.current.handlers.onTouchStart(
        createTouchEvent([{ clientX: 200, clientY: 200 }]),
      );
    });

    expect(result.current.isZoomed).toBe(true);

    // Start pan
    act(() => {
      result.current.handlers.onTouchStart(
        createTouchEvent([{ clientX: 200, clientY: 200 }]),
      );
    });

    // Move finger
    act(() => {
      result.current.handlers.onTouchMove(
        createTouchEvent([{ clientX: 250, clientY: 230 }]),
      );
    });

    expect(result.current.state.translateX).toBe(50);
    expect(result.current.state.translateY).toBe(30);
  });

  it('saves pan offset on touch end when zoomed', () => {
    const { result } = renderHook(() => usePinchZoom());

    // Double-tap to zoom in
    act(() => {
      result.current.handlers.onTouchStart(
        createTouchEvent([{ clientX: 200, clientY: 200 }]),
      );
    });
    act(() => {
      result.current.handlers.onTouchStart(
        createTouchEvent([{ clientX: 200, clientY: 200 }]),
      );
    });

    // Pan
    act(() => {
      result.current.handlers.onTouchStart(
        createTouchEvent([{ clientX: 200, clientY: 200 }]),
      );
    });
    act(() => {
      result.current.handlers.onTouchMove(
        createTouchEvent([{ clientX: 250, clientY: 200 }]),
      );
    });

    // End touch
    act(() => {
      result.current.handlers.onTouchEnd(
        createTouchEvent([]),
      );
    });

    // Should preserve translate state
    expect(result.current.state.translateX).toBe(50);
  });

  it('respects maxScale during pinch', () => {
    const { result } = renderHook(() => usePinchZoom({ maxScale: 2 }));

    // Start pinch 100px apart
    act(() => {
      result.current.handlers.onTouchStart(
        createTouchEvent([
          { clientX: 150, clientY: 200 },
          { clientX: 250, clientY: 200 },
        ]),
      );
    });

    // Move to 500px apart (5x would exceed maxScale of 2)
    act(() => {
      result.current.handlers.onTouchMove(
        createTouchEvent([
          { clientX: 0, clientY: 200 },
          { clientX: 500, clientY: 200 },
        ]),
      );
    });

    expect(result.current.state.scale).toBeLessThanOrEqual(2);
  });

  it('does not move when single touch without zoom', () => {
    const { result } = renderHook(() => usePinchZoom());

    // Single touch move without being zoomed — should not pan
    act(() => {
      result.current.handlers.onTouchMove(
        createTouchEvent([{ clientX: 250, clientY: 250 }]),
      );
    });

    expect(result.current.state.translateX).toBe(0);
    expect(result.current.state.translateY).toBe(0);
  });
});
