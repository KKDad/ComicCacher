'use client';

import { useCallback, useEffect, useState } from 'react';

interface UseLightboxReturn {
  isOpen: boolean;
  currentIndex: number;
  open: (index: number) => void;
  close: () => void;
  next: () => void;
  previous: () => void;
}

export function useLightbox(itemCount: number): UseLightboxReturn {
  const [isOpen, setIsOpen] = useState(false);
  const [currentIndex, setCurrentIndex] = useState(0);

  const open = useCallback((index: number) => {
    setCurrentIndex(index);
    setIsOpen(true);
  }, []);

  const close = useCallback(() => {
    setIsOpen(false);
  }, []);

  const next = useCallback(() => {
    setCurrentIndex((prev) => (prev < itemCount - 1 ? prev + 1 : prev));
  }, [itemCount]);

  const previous = useCallback(() => {
    setCurrentIndex((prev) => (prev > 0 ? prev - 1 : prev));
  }, []);

  // Keyboard navigation when open — captures and stops propagation
  // so the grid reader's date-shifting handlers don't fire
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      switch (e.key) {
        case 'Escape':
          e.preventDefault();
          e.stopPropagation();
          close();
          break;
        case 'ArrowLeft':
          e.preventDefault();
          e.stopPropagation();
          previous();
          break;
        case 'ArrowRight':
          e.preventDefault();
          e.stopPropagation();
          next();
          break;
      }
    };

    // Use capture phase to intercept before grid reader's bubble-phase listener
    window.addEventListener('keydown', handleKeyDown, true);
    return () => window.removeEventListener('keydown', handleKeyDown, true);
  }, [isOpen, close, next, previous]);

  // Lock body scroll when open
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    return () => {
      document.body.style.overflow = '';
    };
  }, [isOpen]);

  return { isOpen, currentIndex, open, close, next, previous };
}
