'use client';

import { useCallback, useMemo } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import {
  useGetUserPreferencesQuery,
  useAddFavoriteMutation,
  useRemoveFavoriteMutation,
} from '@/generated/graphql';

/** The user's favorite comic ids, and a toggle that adds or removes one. */
export function useFavorites() {
  const queryClient = useQueryClient();
  const { data } = useGetUserPreferencesQuery(undefined, { staleTime: 5 * 60 * 1000 });
  const favoriteComics = data?.preferences?.favoriteComics;
  const favoriteIds = useMemo(() => new Set(favoriteComics ?? []), [favoriteComics]);

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['GetUserPreferences'] });
  const add = useAddFavoriteMutation({
    onSuccess: (result) => {
      if (result.addFavorite.errors.length === 0) refresh();
    },
  });
  const remove = useRemoveFavoriteMutation({
    onSuccess: (result) => {
      if (result.removeFavorite.errors.length === 0) refresh();
    },
  });

  const { mutate: addFavorite } = add;
  const { mutate: removeFavorite } = remove;
  const toggle = useCallback(
    (comicId: number) => {
      if (favoriteIds.has(comicId)) {
        removeFavorite({ comicId });
      } else {
        addFavorite({ comicId });
      }
    },
    [favoriteIds, addFavorite, removeFavorite],
  );

  return { favoriteIds, toggle, isPending: add.isPending || remove.isPending };
}

/** Whether one comic is a favorite, and a toggle for it. */
export function useFavorite(comicId: number) {
  const { favoriteIds, toggle, isPending } = useFavorites();
  const toggleThis = useCallback(() => toggle(comicId), [toggle, comicId]);
  return { isFavorite: favoriteIds.has(comicId), toggle: toggleThis, isPending };
}
