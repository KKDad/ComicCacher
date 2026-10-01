'use client';

import { useEffect, useRef, useState } from 'react';
import { useRequestCatalogThumbnailsMutation } from '@/generated/graphql';
import type { CatalogEntry } from '@/types/sources';

/** The backend queues at most 100 per request. */
const MAX_PER_REQUEST = 100;
const DEBOUNCE_MS = 500;
/** After a failed request, wait this long before asking again. */
export const RETRY_MS = 30_000;

/** Entries on screen that have no picture yet and aren't already being fetched. */
export function entriesNeedingThumbnails(entries: CatalogEntry[]): string[] {
  return entries
    .filter((e) => !e.removedAt && !e.thumbnailUrl && !e.thumbnailPending && !e.comic?.avatarAvailable)
    .map((e) => e.identifier);
}

/**
 * Asks the server to download thumbnails for the catalog entries on screen, once
 * each, a moment after they appear. Only what someone is looking at is fetched.
 */
export function useCatalogThumbnails(sourceId: string, visible: CatalogEntry[], onQueued: () => void) {
  const requested = useRef(new Set<string>());
  const queuedCallback = useRef(onQueued);
  const [failures, setFailures] = useState(0);
  const { mutate } = useRequestCatalogThumbnailsMutation();

  useEffect(() => {
    queuedCallback.current = onQueued;
  }, [onQueued]);
  const wantedKey = entriesNeedingThumbnails(visible).join(',');

  useEffect(() => {
    if (!wantedKey) return;
    const timer = setTimeout(
      () => {
        const identifiers = wantedKey
          .split(',')
          .filter((id) => !requested.current.has(id))
          .slice(0, MAX_PER_REQUEST);
        if (identifiers.length === 0) return;
        identifiers.forEach((id) => requested.current.add(id));
        mutate(
          { source: sourceId, identifiers },
          {
            onSuccess: (data) => {
              setFailures(0);
              if (data.requestCatalogThumbnails.queued > 0) {
                queuedCallback.current();
              }
            },
            onError: () => {
              identifiers.forEach((id) => requested.current.delete(id));
              setFailures((n) => n + 1);
            },
          },
        );
      },
      failures > 0 ? RETRY_MS : DEBOUNCE_MS,
    );
    return () => clearTimeout(timer);
  }, [sourceId, wantedKey, mutate, failures]);
}
