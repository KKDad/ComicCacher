'use client';

import { useSyncExternalStore } from 'react';

const subscribe = () => () => {};

/**
 * False during server rendering and hydration, true afterwards. Use it to hold back
 * client-only state (cached queries, storage) so the first client render matches the
 * server HTML.
 */
export function useHydrated(): boolean {
  return useSyncExternalStore(
    subscribe,
    () => true,
    () => false,
  );
}
