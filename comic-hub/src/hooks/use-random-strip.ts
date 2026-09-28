'use client';

import { useCallback, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { useGetRandomStripQuery } from '@/generated/graphql';

/**
 * Jumps to a random strip of a comic. Fetches in the click handler rather than
 * through an enabled query plus an effect, so each click gets a fresh strip.
 */
export function useRandomStrip(goToDate: (date: string) => void) {
  const queryClient = useQueryClient();
  const [isLoadingRandom, setIsLoadingRandom] = useState(false);

  const goToRandom = useCallback(
    async (comicId: number) => {
      const variables = { comicId };
      setIsLoadingRandom(true);
      try {
        const data = await queryClient.fetchQuery({
          queryKey: useGetRandomStripQuery.getKey(variables),
          queryFn: useGetRandomStripQuery.fetcher(variables),
          staleTime: 0, // A new random strip every time
        });
        if (data.randomStrip) goToDate(data.randomStrip.date);
      } catch {
        toast.error('Could not load a random strip');
      } finally {
        setIsLoadingRandom(false);
      }
    },
    [queryClient, goToDate],
  );

  return { goToRandom, isLoadingRandom };
}
