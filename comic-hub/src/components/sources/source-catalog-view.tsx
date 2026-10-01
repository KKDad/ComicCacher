'use client';

import { useMemo, useState } from 'react';
import Link from 'next/link';
import { AlertTriangle, ArrowLeft, RefreshCw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Skeleton } from '@/components/ui/skeleton';
import { CatalogRow, type CatalogRowItem } from './catalog-row';
import { StartDateDialog } from './start-date-dialog';
import { useCatalogThumbnails } from '@/hooks/use-catalog-thumbnails';
import { useSourceActions } from '@/hooks/use-source-actions';
import { useSourceCatalog } from '@/hooks/use-source-catalog';
import { useUser } from '@/contexts/user-context';
import { isAdmin } from '@/lib/roles';
import { formatAbsoluteTime, formatRelativeTime } from '@/lib/date-utils';
import { cn } from '@/lib/utils';
import type { CatalogEntry, SourceComic } from '@/types/sources';

/** Rows shown at once. Thumbnails are only asked for the rows on screen. */
export const PAGE_SIZE = 50;

const FILTERS = [
  { id: 'all', label: 'All' },
  { id: 'configured', label: 'Configured' },
  { id: 'not-configured', label: 'Not configured' },
  { id: 'removed', label: 'No longer listed' },
] as const;
type FilterId = (typeof FILTERS)[number]['id'];

function matchesFilter(entry: CatalogEntry, filter: FilterId): boolean {
  switch (filter) {
    case 'configured':
      return !!entry.comic;
    case 'not-configured':
      return !entry.comic && !entry.removedAt;
    case 'removed':
      return !!entry.removedAt;
    default:
      return true;
  }
}

function matchesSearch(entry: CatalogEntry, query: string): boolean {
  if (!query) return true;
  const q = query.toLowerCase();
  return [entry.name, entry.author, entry.identifier, entry.comic?.name].some((v) => v?.toLowerCase().includes(q));
}

function orphanItem(comic: SourceComic): CatalogRowItem {
  return { identifier: comic.sourceIdentifier ?? String(comic.id), name: comic.name, comic };
}

/** A source's catalog: every comic it offers, with switches to download and show each one. */
export function SourceCatalogView({ sourceId }: { sourceId: string }) {
  const user = useUser();
  const canChange = isAdmin(user?.roles ?? []);
  const { source, entries, orphans, isLoading, error, notFound, markBusy } = useSourceCatalog(sourceId);
  const actions = useSourceActions();
  const [filter, setFilter] = useState<FilterId>('all');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [startComic, setStartComic] = useState<SourceComic | null>(null);

  const filtered = useMemo(
    () => entries.filter((e) => matchesFilter(e, filter) && matchesSearch(e, query.trim())),
    [entries, filter, query],
  );
  const pageCount = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const currentPage = Math.min(page, pageCount - 1);
  const visible = filtered.slice(currentPage * PAGE_SIZE, (currentPage + 1) * PAGE_SIZE);
  useCatalogThumbnails(sourceId, visible, markBusy);

  const counts = useMemo(() => {
    const result: Record<FilterId, number> = { all: entries.length, configured: 0, 'not-configured': 0, removed: 0 };
    for (const e of entries) {
      if (matchesFilter(e, 'configured')) result.configured++;
      if (matchesFilter(e, 'not-configured')) result['not-configured']++;
      if (matchesFilter(e, 'removed')) result.removed++;
    }
    return result;
  }, [entries]);

  const numbered = source?.kind === 'INDEXED';
  const rowActions = {
    onDownloadingChange: (item: CatalogRowItem, on: boolean) => {
      if (item.comic) {
        actions.updateComic(item.comic.id, { active: on });
      } else if (on) {
        actions.addComic(sourceId, item.identifier, true, true);
        markBusy();
      }
    },
    onVisibleChange: (item: CatalogRowItem, on: boolean) => {
      if (item.comic) {
        actions.updateComic(item.comic.id, { enabled: on });
      } else if (on) {
        actions.addComic(sourceId, item.identifier, false, true);
        markBusy();
      }
    },
    onBackfill: (comic: SourceComic) => actions.backfillComic(comic.id),
    onFetchAvatar: (comic: SourceComic) => {
      actions.fetchAvatar(comic.id);
      markBusy();
    },
    onEditStart: (comic: SourceComic) => setStartComic(comic),
  };

  if (isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-10 w-full" />
        {Array.from({ length: 6 }).map((_, i) => (
          <Skeleton key={i} className="h-16 w-full" />
        ))}
      </div>
    );
  }

  if (error || notFound || !source) {
    return (
      <div className="space-y-4">
        <BackLink />
        <p className="text-destructive">{error ? `Failed to load the catalog: ${(error as Error).message}` : `No source called "${sourceId}".`}</p>
      </div>
    );
  }

  // Keep the dialog in step with the latest data for the comic it edits
  const editing = startComic
    ? (entries.find((e) => e.comic?.id === startComic.id)?.comic ?? orphans.find((c) => c.id === startComic.id) ?? startComic)
    : null;

  return (
    <div className="space-y-6">
      <div className="space-y-2">
        <BackLink />
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold">{source.displayName}</h1>
            <p className="text-muted-foreground">
              {source.configuredCount} of {source.catalogCount} comics configured · {source.activeCount} downloading
              {source.lastRefreshed && (
                <>
                  {' '}
                  · catalog read{' '}
                  <time dateTime={source.lastRefreshed} title={formatAbsoluteTime(source.lastRefreshed)}>
                    {formatRelativeTime(source.lastRefreshed)}
                  </time>
                </>
              )}
            </p>
          </div>
          {canChange && source.hasCatalog && (
            <Button
              variant="outline"
              onClick={() => {
                actions.refreshCatalog(sourceId);
                markBusy();
              }}
              disabled={source.refreshing || actions.isRefreshing}
            >
              <RefreshCw className={source.refreshing ? 'animate-spin' : undefined} aria-hidden />
              {source.refreshing ? 'Refreshing…' : 'Refresh catalog'}
            </Button>
          )}
        </div>
        {source.lastRefreshError && !source.refreshing && (
          <p className="flex items-start gap-1 text-sm text-destructive">
            <AlertTriangle className="mt-0.5 size-4 shrink-0" aria-hidden />
            <span className="break-words">Last refresh failed: {source.lastRefreshError}</span>
          </p>
        )}
      </div>

      {entries.length === 0 && source.hasCatalog ? (
        <Card>
          <CardContent className="py-8 text-center text-muted-foreground">
            The catalog hasn&apos;t been read yet.{canChange ? ' Use Refresh catalog to read it now.' : ''}
          </CardContent>
        </Card>
      ) : (
        <Card className="gap-0 py-0">
          <div className="flex flex-col gap-3 border-b p-4 sm:flex-row sm:items-center sm:justify-between">
            <div role="group" aria-label="Show" className="flex flex-wrap gap-2">
              {FILTERS.map((f) => (
                <button
                  key={f.id}
                  type="button"
                  aria-pressed={filter === f.id}
                  onClick={() => {
                    setFilter(f.id);
                    setPage(0);
                  }}
                  className={cn(
                    'rounded-full border px-3 py-1 text-sm',
                    filter === f.id ? 'border-primary bg-primary text-primary-foreground' : 'hover:bg-accent',
                  )}
                >
                  {f.label} <span className="opacity-75">{counts[f.id]}</span>
                </button>
              ))}
            </div>
            <Input
              type="search"
              placeholder="Search name, author or slug"
              aria-label="Search the catalog"
              value={query}
              onChange={(e) => {
                setQuery(e.target.value);
                setPage(0);
              }}
              className="sm:max-w-64"
            />
          </div>

          {visible.length === 0 ? (
            <p className="p-8 text-center text-muted-foreground">No comics match.</p>
          ) : (
            <ul aria-label={`${source.displayName} comics`}>
              {visible.map((entry) => (
                <CatalogRow key={entry.identifier} item={entry} numbered={numbered} canChange={canChange} busy={actions.isSaving} {...rowActions} />
              ))}
            </ul>
          )}

          {pageCount > 1 && (
            <div className="flex items-center justify-between gap-2 border-t p-4 text-sm">
              <span className="text-muted-foreground">
                {currentPage * PAGE_SIZE + 1}–{Math.min((currentPage + 1) * PAGE_SIZE, filtered.length)} of {filtered.length}
              </span>
              <div className="flex gap-2">
                <Button size="sm" variant="outline" onClick={() => setPage(currentPage - 1)} disabled={currentPage === 0}>
                  Previous
                </Button>
                <Button size="sm" variant="outline" onClick={() => setPage(currentPage + 1)} disabled={currentPage >= pageCount - 1}>
                  Next
                </Button>
              </div>
            </div>
          )}
        </Card>
      )}

      {orphans.length > 0 && (
        <Card className="gap-0 pb-0">
          <CardHeader className="pb-4">
            <CardTitle>Not in the catalog</CardTitle>
            <CardDescription>Configured {source.displayName} comics the source doesn&apos;t list. Check their identifiers, or switch them off.</CardDescription>
          </CardHeader>
          <ul aria-label="Comics not in the catalog" className="border-t">
            {orphans.map((comic) => (
              <CatalogRow key={comic.id} item={orphanItem(comic)} numbered={numbered} canChange={canChange} busy={actions.isSaving} {...rowActions} />
            ))}
          </ul>
        </Card>
      )}

      {editing && (
        <StartDateDialog
          key={editing.id}
          comic={editing}
          numbered={numbered}
          canDetect={source.canDetectStart}
          open
          onOpenChange={(open) => !open && setStartComic(null)}
          onSave={(value) => {
            actions.updateComic(editing.id, value);
            setStartComic(null);
          }}
          onDetect={() => {
            actions.detectStart(editing.id);
            markBusy();
          }}
        />
      )}
    </div>
  );
}

function BackLink() {
  return (
    <Link href="/sources" className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground">
      <ArrowLeft className="size-4" aria-hidden /> Sources
    </Link>
  );
}
