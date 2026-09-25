import { useSyncExternalStore } from 'react';

export type NavLayout = 'desktop' | 'tablet' | 'mobile';

// Keep in step with Tailwind's md (768px) and lg (1024px) breakpoints, which
// the dashboard shell uses to show the matching navigation in CSS.
const DESKTOP_QUERY = '(min-width: 1024px)';
const TABLET_QUERY = '(min-width: 768px)';

function subscribe(onChange: () => void) {
  const queries = [window.matchMedia(DESKTOP_QUERY), window.matchMedia(TABLET_QUERY)];
  queries.forEach((q) => q.addEventListener('change', onChange));
  return () => queries.forEach((q) => q.removeEventListener('change', onChange));
}

function getSnapshot(): NavLayout {
  if (window.matchMedia(DESKTOP_QUERY).matches) return 'desktop';
  if (window.matchMedia(TABLET_QUERY).matches) return 'tablet';
  return 'mobile';
}

/** The server can't know the viewport, so it (and hydration) sees `null`. */
function getServerSnapshot(): NavLayout | null {
  return null;
}

/**
 * Viewport class for components that must render different trees per device
 * (the readers). Returns `null` until the browser has measured, so callers can
 * show a neutral skeleton instead of guessing and then jumping. Layout that
 * can be expressed in CSS should use Tailwind breakpoints instead.
 */
export function useResponsiveNav(): { layout: NavLayout | null } {
  const layout = useSyncExternalStore<NavLayout | null>(subscribe, getSnapshot, getServerSnapshot);
  return { layout };
}
