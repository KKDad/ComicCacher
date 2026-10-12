import { useSyncExternalStore } from 'react';

// A phone on its side: wider than the md breakpoint, but short and touch-driven. Tablets
// are taller than 500px either way round.
export const PHONE_LANDSCAPE_QUERY =
  '(orientation: landscape) and (max-height: 500px) and (pointer: coarse)';

function subscribe(onChange: () => void) {
  const query = window.matchMedia(PHONE_LANDSCAPE_QUERY);
  query.addEventListener('change', onChange);
  return () => query.removeEventListener('change', onChange);
}

const getSnapshot = () => window.matchMedia(PHONE_LANDSCAPE_QUERY).matches;

/** True while a phone is held sideways. False on the server and during hydration. */
export function usePhoneLandscape(): boolean {
  return useSyncExternalStore(subscribe, getSnapshot, () => false);
}
