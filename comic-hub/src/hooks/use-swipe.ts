'use client';

import { useCallback, useRef } from 'react';

interface UseSwipeOptions {
  onSwipeUp?: () => void;
  onSwipeDown?: () => void;
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
  threshold = 50,
}: UseSwipeOptions): SwipeHandlers {
  const startY = useRef(0);
  const startX = useRef(0);
  const deltaY = useRef(0);

  const prefersReducedMotion =
    typeof window !== 'undefined' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  const onTouchStart = useCallback((e: React.TouchEvent) => {
    const touch = e.touches[0];
    startY.current = touch.clientY;
    startX.current = touch.clientX;
    deltaY.current = 0;
  }, []);

  const onTouchMove = useCallback((e: React.TouchEvent) => {
    const touch = e.touches[0];
    deltaY.current = touch.clientY - startY.current;
  }, []);

  const onTouchEnd = useCallback(() => {
    const absDeltaY = Math.abs(deltaY.current);

    // Only fire if vertical swipe is dominant
    if (absDeltaY < threshold) return;
    if (prefersReducedMotion) return;

    if (deltaY.current < -threshold) {
      // Swiped up → newer strip
      onSwipeUp?.();
    } else if (deltaY.current > threshold) {
      // Swiped down → older strip
      onSwipeDown?.();
    }
  }, [threshold, onSwipeUp, onSwipeDown, prefersReducedMotion]);

  return { onTouchStart, onTouchMove, onTouchEnd };
}
