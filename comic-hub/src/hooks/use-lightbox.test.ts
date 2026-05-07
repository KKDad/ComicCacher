import { renderHook, act } from '@testing-library/react';
import { fireEvent } from '@testing-library/react';
import { useLightbox } from './use-lightbox';

describe('useLightbox', () => {
  it('starts closed', () => {
    const { result } = renderHook(() => useLightbox(5));
    expect(result.current.isOpen).toBe(false);
    expect(result.current.currentIndex).toBe(0);
  });

  it('opens at specified index', () => {
    const { result } = renderHook(() => useLightbox(5));
    act(() => result.current.open(3));
    expect(result.current.isOpen).toBe(true);
    expect(result.current.currentIndex).toBe(3);
  });

  it('closes', () => {
    const { result } = renderHook(() => useLightbox(5));
    act(() => result.current.open(2));
    act(() => result.current.close());
    expect(result.current.isOpen).toBe(false);
  });

  it('navigates to next item', () => {
    const { result } = renderHook(() => useLightbox(5));
    act(() => result.current.open(1));
    act(() => result.current.next());
    expect(result.current.currentIndex).toBe(2);
  });

  it('navigates to previous item', () => {
    const { result } = renderHook(() => useLightbox(5));
    act(() => result.current.open(3));
    act(() => result.current.previous());
    expect(result.current.currentIndex).toBe(2);
  });

  it('clamps at last item', () => {
    const { result } = renderHook(() => useLightbox(3));
    act(() => result.current.open(2));
    act(() => result.current.next());
    expect(result.current.currentIndex).toBe(2);
  });

  it('clamps at first item', () => {
    const { result } = renderHook(() => useLightbox(3));
    act(() => result.current.open(0));
    act(() => result.current.previous());
    expect(result.current.currentIndex).toBe(0);
  });

  it('responds to Escape key when open', () => {
    const { result } = renderHook(() => useLightbox(5));
    act(() => result.current.open(1));
    act(() => {
      fireEvent.keyDown(window, { key: 'Escape' });
    });
    expect(result.current.isOpen).toBe(false);
  });

  it('responds to ArrowRight key when open', () => {
    const { result } = renderHook(() => useLightbox(5));
    act(() => result.current.open(1));
    act(() => {
      fireEvent.keyDown(window, { key: 'ArrowRight' });
    });
    expect(result.current.currentIndex).toBe(2);
  });

  it('responds to ArrowLeft key when open', () => {
    const { result } = renderHook(() => useLightbox(5));
    act(() => result.current.open(3));
    act(() => {
      fireEvent.keyDown(window, { key: 'ArrowLeft' });
    });
    expect(result.current.currentIndex).toBe(2);
  });

  it('does not respond to keys when closed', () => {
    const { result } = renderHook(() => useLightbox(5));
    // Don't open — just fire key
    act(() => {
      fireEvent.keyDown(window, { key: 'ArrowRight' });
    });
    expect(result.current.currentIndex).toBe(0);
  });

  it('locks body scroll when open', () => {
    const { result } = renderHook(() => useLightbox(5));
    act(() => result.current.open(0));
    expect(document.body.style.overflow).toBe('hidden');
    act(() => result.current.close());
    expect(document.body.style.overflow).toBe('');
  });
});
