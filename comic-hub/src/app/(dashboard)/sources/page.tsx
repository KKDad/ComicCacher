'use client';

import { useState } from 'react';
import { useGetSourcesQuery } from '@/generated/graphql';
import { Card } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { SourceCard } from '@/components/sources/source-card';
import { useSourceActions } from '@/hooks/use-source-actions';
import { useUser } from '@/contexts/user-context';
import { isAdmin } from '@/lib/roles';

/** After a refresh is asked for, look again this often, for this long. */
const POLL_MS = 3000;
const POLL_FOR_MS = 2 * 60_000;

export default function SourcesPage() {
  const user = useUser();
  const canChange = isAdmin(user?.roles ?? []);
  const actions = useSourceActions();
  const [pollUntil, setPollUntil] = useState(0);
  const { data, isLoading, error } = useGetSourcesQuery(undefined, {
    refetchInterval: (query) => {
      const refreshing = query.state.data?.sources.some((s) => s.refreshing) ?? false;
      return refreshing || Date.now() < pollUntil ? POLL_MS : false;
    },
  });
  const sources = data?.sources ?? [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Sources</h1>
        <p className="mt-1 text-muted-foreground">Where comics come from. Browse each source&apos;s comics to add them, or switch them off.</p>
      </div>

      {error ? (
        <p className="text-destructive">Failed to load sources: {(error as Error).message}</p>
      ) : isLoading ? (
        <div className="grid grid-cols-[repeat(auto-fill,minmax(min(100%,22rem),1fr))] gap-4 items-start">
          {Array.from({ length: 3 }).map((_, i) => (
            <Card key={i} className="p-6">
              <div className="space-y-3">
                <Skeleton className="h-5 w-40" />
                <Skeleton className="h-4 w-56" />
                <Skeleton className="h-8 w-full" />
              </div>
            </Card>
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-[repeat(auto-fill,minmax(min(100%,22rem),1fr))] gap-4 items-start">
          {sources.map((source) => (
            <SourceCard
              key={source.id}
              source={source}
              canChange={canChange}
              onRefresh={(id) => {
                actions.refreshCatalog(id);
                setPollUntil(Date.now() + POLL_FOR_MS);
              }}
              onBackfill={actions.backfillSource}
            />
          ))}
        </div>
      )}
    </div>
  );
}
