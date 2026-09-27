'use client';

import { useCallback, useEffect, useRef } from 'react';
import { usePathname, useRouter } from 'next/navigation';

// Counts client-side route changes since the app loaded. With none, the page was opened
// directly (a shared link, a new tab, a refresh), and "back" would leave the app.
let inAppNavigations = 0;

export function hasInAppHistory(): boolean {
  return inAppNavigations > 0;
}

/** Test hook: forget recorded navigations. */
export function resetNavigationHistory() {
  inAppNavigations = 0;
}

/** Records route changes. Mounted once, in Providers. */
export function NavigationTracker() {
  const pathname = usePathname();
  const firstPath = useRef(pathname);

  useEffect(() => {
    if (pathname !== firstPath.current) {
      inAppNavigations += 1;
      firstPath.current = pathname;
    }
  }, [pathname]);

  return null;
}

/** Goes back when the previous page is in the app, otherwise to `fallback`. */
export function useGoBack(fallback: string) {
  const router = useRouter();
  return useCallback(() => {
    if (hasInAppHistory()) {
      router.back();
    } else {
      router.push(fallback);
    }
  }, [router, fallback]);
}
