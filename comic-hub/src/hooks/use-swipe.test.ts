import { renderHook, act } from '@testing-library/react';
import { useSwipe } from './use-swipe';

function createTouchEvent(clientY: number, clientX = 0): React.TouchEvent {
  return {
    touches: [{ clientY, clientX } as React.Touch],
  } as React.TouchEvent;
}

function createEmptyTouchEvent(): React.TouchEvent {
  return { touches: [] as unknown as React.TouchList } as React.TouchEvent;
}

describe('useSwipe', () => {
  it('returns touch handler functions', () => {
    const { result } = renderHook(() =>
      useSwipe({ onSwipeUp: vi.fn(), onSwipeDown: vi.fn() }),
    );

    expect(result.current.onTouchStart).toBeInstanceOf(Function);
    expect(result.current.onTouchMove).toBeInstanceOf(Function);
    expect(result.current.onTouchEnd).toBeInstanceOf(Function);
  });

  it('calls onSwipeUp when swiped up beyond threshold', () => {
    const onSwipeUp = vi.fn();
    const { result } = renderHook(() =>
      useSwipe({ onSwipeUp, threshold: 50 }),
    );

    act(() => {
      result.current.onTouchStart(createTouchEvent(200));
      result.current.onTouchMove(createTouchEvent(100)); // deltaY = -100
      result.current.onTouchEnd(createEmptyTouchEvent());
    });

    expect(onSwipeUp).toHaveBeenCalledOnce();
  });

  it('calls onSwipeDown when swiped down beyond threshold', () => {
    const onSwipeDown = vi.fn();
    const { result } = renderHook(() =>
      useSwipe({ onSwipeDown, threshold: 50 }),
    );

    act(() => {
      result.current.onTouchStart(createTouchEvent(100));
      result.current.onTouchMove(createTouchEvent(200)); // deltaY = +100
      result.current.onTouchEnd(createEmptyTouchEvent());
    });

    expect(onSwipeDown).toHaveBeenCalledOnce();
  });

  it('does not fire when below threshold', () => {
    const onSwipeUp = vi.fn();
    const onSwipeDown = vi.fn();
    const { result } = renderHook(() =>
      useSwipe({ onSwipeUp, onSwipeDown, threshold: 50 }),
    );

    act(() => {
      result.current.onTouchStart(createTouchEvent(100));
      result.current.onTouchMove(createTouchEvent(80)); // deltaY = -20, below threshold
      result.current.onTouchEnd(createEmptyTouchEvent());
    });

    expect(onSwipeUp).not.toHaveBeenCalled();
    expect(onSwipeDown).not.toHaveBeenCalled();
  });

  it('does not fire when callback is undefined', () => {
    const { result } = renderHook(() => useSwipe({ threshold: 50 }));

    // Should not throw
    act(() => {
      result.current.onTouchStart(createTouchEvent(200));
      result.current.onTouchMove(createTouchEvent(100));
      result.current.onTouchEnd(createEmptyTouchEvent());
    });
  });

  it('uses default threshold of 50', () => {
    const onSwipeUp = vi.fn();
    const { result } = renderHook(() => useSwipe({ onSwipeUp }));

    // 49px delta — below default threshold
    act(() => {
      result.current.onTouchStart(createTouchEvent(100));
      result.current.onTouchMove(createTouchEvent(51));
      result.current.onTouchEnd(createEmptyTouchEvent());
    });
    expect(onSwipeUp).not.toHaveBeenCalled();

    // 51px delta — above default threshold
    act(() => {
      result.current.onTouchStart(createTouchEvent(100));
      result.current.onTouchMove(createTouchEvent(49));
      result.current.onTouchEnd(createEmptyTouchEvent());
    });
    expect(onSwipeUp).toHaveBeenCalledOnce();
  });
});
