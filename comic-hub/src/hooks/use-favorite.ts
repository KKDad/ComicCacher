'use client';

import { useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import {
  useGetUserPreferencesQuery,
  useAddFavoriteMutation,
  useRemoveFavoriteMutation,
} from '@/generated/graphql';

/** Whether a comic is a favorite, and a toggle that adds or removes it. */
export function useFavorite(comicId: number) {
  const queryClient = useQueryClient();
  const { data } = useGetUserPreferencesQuery(undefined, { staleTime: 5 * 60 * 1000 });
  const isFavorite = data?.preferences?.favoriteComics.includes(comicId) ?? false;

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
  const toggle = useCallback(() => {
    if (isFavorite) {
      removeFavorite({ comicId });
    } else {
      addFavorite({ comicId });
    }
  }, [isFavorite, comicId, addFavorite, removeFavorite]);

  return { isFavorite, toggle, isPending: add.isPending || remove.isPending };
}
