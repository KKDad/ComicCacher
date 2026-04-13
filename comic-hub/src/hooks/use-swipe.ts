'use client';

import { useCallback, useRef } from 'react';

interface UseSwipeOptions {
  onSwipeUp?: () => void;
  onSwipeDown?: () => void;
  onSwipeLeft?: () => void;
  onSwipeRight?: () => void;
  threshold?: number;
}

interface SwipeHandlers {
  onTouchStart: (e: React.TouchEvent) => void;
  onTouchMove: (e: React.TouchEvent) => void;
  onTouchEnd: (e: React.TouchEvent) => void;
}

export function useSwipe({
  onSwipeUp,
  onSwipeDown,
  onSwipeLeft,
  onSwipeRight,
  threshold = 50,
}: UseSwipeOptions): SwipeHandlers {
  const startY = useRef(0);
  const startX = useRef(0);
  const deltaY = useRef(0);
  const deltaX = useRef(0);

  const prefersReducedMotion =
    typeof window !== 'undefined' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  const onTouchStart = useCallback((e: React.TouchEvent) => {
    const touch = e.touches[0];
    startY.current = touch.clientY;
    startX.current = touch.clientX;
    deltaY.current = 0;
    deltaX.current = 0;
  }, []);

  const onTouchMove = useCallback((e: React.TouchEvent) => {
    const touch = e.touches[0];
    deltaY.current = touch.clientY - startY.current;
    deltaX.current = touch.clientX - startX.current;
  }, []);

  const onTouchEnd = useCallback(() => {
    if (prefersReducedMotion) return;

    const absDeltaX = Math.abs(deltaX.current);
    const absDeltaY = Math.abs(deltaY.current);

    // Determine dominant axis
    if (absDeltaX > absDeltaY && absDeltaX >= threshold) {
      // Horizontal swipe is dominant
      if (deltaX.current < -threshold) {
        onSwipeLeft?.();
      } else if (deltaX.current > threshold) {
        onSwipeRight?.();
      }
    } else if (absDeltaY >= threshold) {
      // Vertical swipe is dominant
      if (deltaY.current < -threshold) {
        onSwipeUp?.();
      } else if (deltaY.current > threshold) {
        onSwipeDown?.();
      }
    }
  }, [threshold, onSwipeUp, onSwipeDown, onSwipeLeft, onSwipeRight, prefersReducedMotion]);

  return { onTouchStart, onTouchMove, onTouchEnd };
}
