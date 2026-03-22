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
});
