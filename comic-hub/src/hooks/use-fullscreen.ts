'use client';

import { useCallback, useEffect, useSyncExternalStore } from 'react';
import { useHydrated } from './use-hydrated';

function subscribe(onChange: () => void) {
  document.addEventListener('fullscreenchange', onChange);
  return () => document.removeEventListener('fullscreenchange', onChange);
}

const isFullscreenNow = () => document.fullscreenElement != null;

/** Resolves false when the browser refuses, e.g. because there was no user gesture. */
async function enterFullscreen(): Promise<boolean> {
  if (document.fullscreenElement) return true;
  try {
    await document.documentElement.requestFullscreen({ navigationUI: 'hide' });
    return true;
  } catch (e) {
    console.debug('requestFullscreen failed', e);
    return false;
  }
}

function exitFullscreen() {
  if (!document.fullscreenElement) return;
  document.exitFullscreen().catch((e: unknown) => console.debug('exitFullscreen failed', e));
}

/**
 * Full screen for the whole page through the Fullscreen API. `supported` is null until
 * hydration and false where the browser has no API (iPhone Safari), so callers can hide
 * their toggle. Leaves full screen when the component unmounts, so navigating away from
 * the reader doesn't keep the rest of the app full screen.
 */
export function useFullscreen() {
  const hydrated = useHydrated();
  const isFullscreen = useSyncExternalStore(subscribe, isFullscreenNow, () => false);

  const supported = hydrated
    ? Boolean(document.fullscreenEnabled && document.documentElement.requestFullscreen)
    : null;

  const toggle = useCallback(() => {
    if (document.fullscreenElement) exitFullscreen();
    else void enterFullscreen();
  }, []);

  useEffect(() => exitFullscreen, []);

  return { supported, isFullscreen, toggle, enter: enterFullscreen, exit: exitFullscreen };
}
