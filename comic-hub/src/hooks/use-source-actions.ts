'use client';

import { useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import {
  useAddComicFromCatalogMutation,
  useDetectComicStartMutation,
  useFetchComicAvatarMutation,
  useRefreshSourceCatalogMutation,
  useTriggerJobMutation,
  useUpdateSourceComicMutation,
  type UpdateComicInput,
} from '@/generated/graphql';

/** Query keys whose data a change to a source or comic makes stale. */
const STALE_PREFIXES = ['GetSources', 'GetSourceCatalog', 'GetComics', 'GetComic', 'SearchComics'];

/**
 * The Sources page's changes, each with its toast and a refresh of the source,
 * catalog and library queries it affects.
 */
export function useSourceActions() {
  const queryClient = useQueryClient();
  const refresh = () =>
    queryClient.invalidateQueries({
      predicate: (query) => STALE_PREFIXES.some((prefix) => String(query.queryKey[0]).startsWith(prefix)),
    });
  const fail = (what: string) => (error: Error) => toast.error(`${what}: ${error.message}`);

  const addComic = useAddComicFromCatalogMutation({
    onSuccess: (data) => {
      const { comic, errors } = data.addComicFromCatalog;
      if (errors.length > 0 || !comic) {
        toast.error(errors[0]?.message ?? 'The comic could not be added');
        return;
      }
      toast.success(`${comic.name} added`);
      refresh();
    },
    onError: fail('Could not add the comic'),
  });

  const updateComic = useUpdateSourceComicMutation({
    onSuccess: (data) => {
      const { comic, errors } = data.updateComic;
      if (errors.length > 0 || !comic) {
        toast.error(errors[0]?.message ?? 'The comic could not be saved');
        return;
      }
      refresh();
    },
    onError: fail('Could not save the comic'),
  });

  const refreshCatalog = useRefreshSourceCatalogMutation({
    onSuccess: (data) => {
      const { errors } = data.refreshSourceCatalog;
      if (errors.length > 0) {
        toast.error(errors[0].message);
        return;
      }
      toast.success('Catalog refresh started');
      refresh();
    },
    onError: fail('Could not refresh the catalog'),
  });

  const backfill = useTriggerJobMutation({
    onSuccess: (data) => {
      const { errors } = data.triggerJob;
      if (errors.length > 0) {
        toast.error(errors[0].message);
        return;
      }
      toast.success('Backfill started; follow it on the Batch Jobs page');
    },
    onError: fail('Could not start the backfill'),
  });

  const fetchAvatar = useFetchComicAvatarMutation({
    onSuccess: (data) => {
      const { queued, errors } = data.fetchComicAvatar;
      if (!queued) {
        toast.error(errors[0]?.message ?? 'The avatar could not be queued');
        return;
      }
      toast.success('Avatar download queued');
      refresh();
    },
    onError: fail('Could not fetch the avatar'),
  });

  const detectStart = useDetectComicStartMutation({
    onSuccess: (data) => {
      const { queued, errors } = data.detectComicStart;
      if (!queued) {
        toast.error(errors[0]?.message ?? 'Start detection could not be queued');
        return;
      }
      toast.success('Checking the source for the first strip');
      refresh();
    },
    onError: fail('Could not detect the start'),
  });

  return {
    addComic: (source: string, identifier: string, active: boolean, enabled: boolean) =>
      addComic.mutate({ input: { source, identifier, active, enabled } }),
    updateComic: (id: number, input: UpdateComicInput) => updateComic.mutate({ id, input }),
    refreshCatalog: (source: string) => refreshCatalog.mutate({ source }),
    backfillSource: (source: string) => backfill.mutate({ jobName: 'ComicBackfillJob', parameters: { source } }),
    backfillComic: (comicId: number) => backfill.mutate({ jobName: 'ComicBackfillJob', parameters: { comic: String(comicId) } }),
    fetchAvatar: (id: number) => fetchAvatar.mutate({ id }),
    detectStart: (id: number) => detectStart.mutate({ id }),
    isSaving: addComic.isPending || updateComic.isPending,
    isRefreshing: refreshCatalog.isPending,
  };
}
