import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useResponsiveNav } from './use-responsive-nav';

describe('use-responsive-nav', () => {
  let originalInnerWidth: number;

  beforeEach(() => {
    originalInnerWidth = window.innerWidth;
  });

  afterEach(() => {
    // Restore original width
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: originalInnerWidth,
    });
  });

  const setWindowWidth = (width: number) => {
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: width,
    });
  };

  it('returns desktop layout when width >= 1024', () => {
    setWindowWidth(1024);
    const { result } = renderHook(() => useResponsiveNav());
    expect(result.current.layout).toBe('desktop');
  });

  it('returns desktop layout when width > 1024', () => {
    setWindowWidth(1920);
    const { result } = renderHook(() => useResponsiveNav());
    expect(result.current.layout).toBe('desktop');
  });

  it('returns tablet layout when width >= 768 and < 1024', () => {
    setWindowWidth(768);
    const { result } = renderHook(() => useResponsiveNav());
    expect(result.current.layout).toBe('tablet');
  });

  it('returns tablet layout when width is between 768 and 1023', () => {
    setWindowWidth(800);
    const { result } = renderHook(() => useResponsiveNav());
    expect(result.current.layout).toBe('tablet');
  });

  it('returns mobile layout when width < 768', () => {
    setWindowWidth(767);
    const { result } = renderHook(() => useResponsiveNav());
    expect(result.current.layout).toBe('mobile');
  });

  it('returns mobile layout when width is very small', () => {
    setWindowWidth(320);
    const { result } = renderHook(() => useResponsiveNav());
    expect(result.current.layout).toBe('mobile');
  });

  it('updates layout on window resize from desktop to tablet', () => {
    setWindowWidth(1024);
    const { result } = renderHook(() => useResponsiveNav());
    expect(result.current.layout).toBe('desktop');

    act(() => {
      setWindowWidth(800);
      window.dispatchEvent(new Event('resize'));
    });

    expect(result.current.layout).toBe('tablet');
  });

  it('updates layout on window resize from tablet to mobile', () => {
    setWindowWidth(800);
    const { result } = renderHook(() => useResponsiveNav());
    expect(result.current.layout).toBe('tablet');

    act(() => {
      setWindowWidth(500);
      window.dispatchEvent(new Event('resize'));
    });

    expect(result.current.layout).toBe('mobile');
  });

  it('updates layout on window resize from mobile to desktop', () => {
    setWindowWidth(500);
    const { result } = renderHook(() => useResponsiveNav());
    expect(result.current.layout).toBe('mobile');

    act(() => {
      setWindowWidth(1200);
      window.dispatchEvent(new Event('resize'));
    });

    expect(result.current.layout).toBe('desktop');
  });

  it('cleans up resize listener on unmount', () => {
    const removeEventListenerSpy = vi.spyOn(window, 'removeEventListener');
    setWindowWidth(1024);
    const { unmount } = renderHook(() => useResponsiveNav());

    unmount();

    expect(removeEventListenerSpy).toHaveBeenCalledWith('resize', expect.any(Function));
    removeEventListenerSpy.mockRestore();
  });
});
