'use client';

import { useCallback, useRef, useState } from 'react';

interface UsePinchZoomOptions {
  minScale?: number;
  maxScale?: number;
}

interface PinchZoomState {
  scale: number;
  originX: number;
  originY: number;
  translateX: number;
  translateY: number;
}

interface PinchZoomHandlers {
  onTouchStart: (e: React.TouchEvent) => void;
  onTouchMove: (e: React.TouchEvent) => void;
  onTouchEnd: (e: React.TouchEvent) => void;
}

interface UsePinchZoomReturn {
  state: PinchZoomState;
  handlers: PinchZoomHandlers;
  isZoomed: boolean;
  resetZoom: () => void;
}

/** Scale threshold below which the view snaps back to 1x. */
const ZOOM_SNAP_THRESHOLD = 1.05;
/** Maximum interval (ms) between taps to count as a double-tap. */
const DOUBLE_TAP_WINDOW_MS = 300;

function getDistance(t1: React.Touch, t2: React.Touch): number {
  const dx = t1.clientX - t2.clientX;
  const dy = t1.clientY - t2.clientY;
  return Math.sqrt(dx * dx + dy * dy);
}

function getMidpoint(t1: React.Touch, t2: React.Touch) {
  return {
    x: (t1.clientX + t2.clientX) / 2,
    y: (t1.clientY + t2.clientY) / 2,
  };
}

export function usePinchZoom({
  minScale = 0.5,
  maxScale = 4,
}: UsePinchZoomOptions = {}): UsePinchZoomReturn {
  const [state, setState] = useState<PinchZoomState>({
    scale: 1,
    originX: 50,
    originY: 50,
    translateX: 0,
    translateY: 0,
  });

  const initialDistance = useRef(0);
  const initialScale = useRef(1);
  const lastTapTime = useRef(0);
  const isPinching = useRef(false);
  const panStart = useRef({ x: 0, y: 0 });
  const panOffset = useRef({ x: 0, y: 0 });

  const isZoomed = state.scale > ZOOM_SNAP_THRESHOLD;

  const resetZoom = useCallback(() => {
    setState({ scale: 1, originX: 50, originY: 50, translateX: 0, translateY: 0 });
    panOffset.current = { x: 0, y: 0 };
  }, []);

  const onTouchStart = useCallback((e: React.TouchEvent) => {
    if (e.touches.length === 2) {
      isPinching.current = true;
      initialDistance.current = getDistance(e.touches[0], e.touches[1]);
      initialScale.current = state.scale;
      const mid = getMidpoint(e.touches[0], e.touches[1]);
      const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
      setState((prev) => ({
        ...prev,
        originX: ((mid.x - rect.left) / rect.width) * 100,
        originY: ((mid.y - rect.top) / rect.height) * 100,
      }));
    } else if (e.touches.length === 1 && isZoomed) {
      // Pan when zoomed
      panStart.current = { x: e.touches[0].clientX, y: e.touches[0].clientY };
    } else if (e.touches.length === 1) {
      // Double-tap detection
      const now = Date.now();
      if (now - lastTapTime.current < DOUBLE_TAP_WINDOW_MS) {
        // Double-tap → toggle zoom
        if (isZoomed) {
          resetZoom();
        } else {
          const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
          const touch = e.touches[0];
          setState({
            scale: 2,
            originX: ((touch.clientX - rect.left) / rect.width) * 100,
            originY: ((touch.clientY - rect.top) / rect.height) * 100,
            translateX: 0,
            translateY: 0,
          });
          panOffset.current = { x: 0, y: 0 };
        }
        lastTapTime.current = 0;
      } else {
        lastTapTime.current = now;
      }
    }
  }, [state.scale, isZoomed, resetZoom]);

  const onTouchMove = useCallback((e: React.TouchEvent) => {
    if (e.touches.length === 2 && isPinching.current) {
      const dist = getDistance(e.touches[0], e.touches[1]);
      const ratio = dist / initialDistance.current;
      const newScale = Math.min(maxScale, Math.max(minScale, initialScale.current * ratio));
      setState((prev) => ({ ...prev, scale: newScale }));
    } else if (e.touches.length === 1 && isZoomed) {
      // Pan
      const dx = e.touches[0].clientX - panStart.current.x;
      const dy = e.touches[0].clientY - panStart.current.y;
      setState((prev) => ({
        ...prev,
        translateX: panOffset.current.x + dx,
        translateY: panOffset.current.y + dy,
      }));
    }
  }, [isZoomed, minScale, maxScale]);

  const onTouchEnd = useCallback((e: React.TouchEvent) => {
    if (isPinching.current && e.touches.length < 2) {
      isPinching.current = false;
      // Snap to 1x if close
      setState((prev) => {
        if (prev.scale < ZOOM_SNAP_THRESHOLD) {
          panOffset.current = { x: 0, y: 0 };
          return { ...prev, scale: 1, translateX: 0, translateY: 0 };
        }
        return prev;
      });
    }
    if (e.touches.length === 0 && isZoomed) {
      panOffset.current = { x: state.translateX, y: state.translateY };
    }
  }, [isZoomed, state.translateX, state.translateY]);

  return {
    state,
    handlers: { onTouchStart, onTouchMove, onTouchEnd },
    isZoomed,
    resetZoom,
  };
}
