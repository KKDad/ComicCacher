'use client';

import { Suspense, useEffect, useMemo } from 'react';
import { usePathname, useRouter, useSearchParams } from 'next/navigation';
import { CheckCircle, RefreshCw } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { useGetRetrievalHealthQuery } from '@/generated/graphql';
import { LastRunBanner } from '@/components/retrieval-status/last-run-banner';
import { SourceHealth } from '@/components/retrieval-status/source-health';
import { TodaysErrors } from '@/components/retrieval-status/todays-errors';
import { ResultsGrid } from '@/components/retrieval-status/results-grid';
import { ComicRetrievalDrawer } from '@/components/retrieval-status/comic-retrieval-drawer';
import { ComicRetrievalPanel } from '@/components/retrieval-status/comic-retrieval-panel';
import { useResponsiveNav } from '@/hooks/use-responsive-nav';
import { RetrievalFilters, type Filters } from '@/components/retrieval-status/retrieval-filters';
import { compareNames } from '@/lib/sort';
import { MAX_WINDOW, WINDOWS, bySeverity, needsAttention, type ComicHealth } from '@/components/retrieval-status/health';

/** Whether a key press came from a control that uses the arrow keys itself. */
function isFromControl(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) return false;
  return target.isContentEditable || target.closest('input, textarea, select, [role="combobox"], [role="switch"], [role="listbox"], [role="dialog"]') != null;
}

/** Reads the filters from the URL, so a filtered view can be linked to. */
function readFilters(params: URLSearchParams): Filters {
  const days = Number(params.get('days'));
  return {
    source: params.get('source'),
    query: params.get('q') ?? '',
    attentionOnly: params.get('attention') === '1',
    days: (WINDOWS as readonly number[]).includes(days) ? days : MAX_WINDOW,
  };
}

function writeFilters(filters: Filters, comicId: number | null): string {
  const params = new URLSearchParams();
  if (filters.source) params.set('source', filters.source);
  if (filters.query) params.set('q', filters.query);
  if (filters.attentionOnly) params.set('attention', '1');
  if (filters.days !== MAX_WINDOW) params.set('days', String(filters.days));
  if (comicId != null) params.set('comic', String(comicId));
  const query = params.toString();
  return query ? `?${query}` : '';
}

function RetrievalStatus() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const filters = readFilters(new URLSearchParams(searchParams.toString()));
  const comicParam = Number(searchParams.get('comic'));
  const selectedId = Number.isInteger(comicParam) && comicParam > 0 ? comicParam : null;
  const { layout } = useResponsiveNav();
  const mobile = layout === 'mobile';

  const { data, isLoading, error } = useGetRetrievalHealthQuery({ days: MAX_WINDOW });
  const health = data?.retrievalHealth;

  const sources = useMemo(() => health?.sources.map((s) => s.source) ?? [], [health]);
  const dates = useMemo(() => health?.comics[0]?.days.slice(-filters.days).map((d) => d.date) ?? [], [health, filters.days]);
  const shown = useMemo(() => {
    const search = filters.query.trim().toLowerCase();
    return (health?.comics ?? [])
      .filter((c) => !filters.attentionOnly || needsAttention(c))
      .filter((c) => !filters.source || c.source === filters.source)
      .filter((c) => !search || c.comicName.toLowerCase().includes(search))
      .toSorted(filters.attentionOnly ? bySeverity : (a, b) => compareNames(a.comicName, b.comicName));
  }, [health, filters.query, filters.attentionOnly, filters.source]);

  const select = (comicId: number | null, next: Filters = filters) =>
    router.replace(`${pathname}${writeFilters(next, comicId)}`, { scroll: false });
  const setFilters = (next: Filters) => select(selectedId, next);
  const selected: ComicHealth | null = health?.comics.find((c) => c.comicId === selectedId) ?? null;
  // The panel is never empty: without a selection it shows the worst comic needing attention, or the first one listed
  const panelComic: ComicHealth | null = useMemo(
    () => selected ?? health?.comics.filter(needsAttention).toSorted(bySeverity)[0] ?? shown[0] ?? null,
    [selected, health, shown],
  );

  // Up and down step through the listed comics, in the panel or the sheet
  useEffect(() => {
    function onKeyDown(e: KeyboardEvent) {
      if ((e.key !== 'ArrowDown' && e.key !== 'ArrowUp') || e.altKey || e.ctrlKey || e.metaKey || shown.length === 0) return;
      if (mobile && selected == null) return;
      if (isFromControl(e.target) && !(mobile && selected)) return;
      const index = shown.findIndex((c) => c.comicId === panelComic?.comicId);
      const next = e.key === 'ArrowDown' ? Math.min(index + 1, shown.length - 1) : Math.max(index - 1, 0);
      e.preventDefault();
      if (shown[next] && shown[next].comicId !== panelComic?.comicId) select(shown[next].comicId);
    }
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  });

  if (isLoading) return <PageSkeleton />;

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center py-20 text-center">
        <h2 className="text-xl font-semibold text-destructive">Failed to load retrieval status</h2>
        <p className="mt-2 text-muted-foreground">{error instanceof Error ? error.message : 'An unexpected error occurred.'}</p>
      </div>
    );
  }

  if (!health || health.comics.length === 0) {
    return (
      <div className="space-y-6">
        <h1 className="text-3xl font-bold text-ink">Retrieval Status</h1>
        <Card className="border-dashed">
          <div className="flex flex-col items-center justify-center p-12 text-center">
            <RefreshCw className="mb-4 h-12 w-12 text-ink-muted" />
            <p className="mb-2 text-ink-subtle">No comics yet</p>
            <p className="text-sm text-ink-muted">Retrieval results appear once comics are added and the daily download runs</p>
          </div>
        </Card>
      </div>
    );
  }

  const attentionCount = health.comics.filter(needsAttention).length;

  return (
    <div className="space-y-6" data-layout="wide">
      <h1 className="text-3xl font-bold text-ink">Retrieval Status</h1>

      <LastRunBanner targetDate={health.targetDate} lastRun={health.lastRun} comics={health.comics} />
      <SourceHealth sources={health.sources} />
      <TodaysErrors errors={health.todaysErrors} />

      <div className="gap-6 md:grid md:grid-cols-[minmax(0,1fr)_18rem] md:items-start 2xl:grid-cols-[minmax(0,1fr)_20rem]">
        <Card className="min-w-0">
          <div className="space-y-4 p-6 pb-4">
            <div>
              <h2 className="text-lg font-semibold text-ink">Results by day</h2>
              <p className="text-sm text-ink-subtle">
                ✓ on disk · ✗ expected and missing · blank when no strip was due. An outlined ✓ was fixed by a later attempt.
                Select a comic for its attempts; ↑ and ↓ step through the list.
              </p>
            </div>
            <RetrievalFilters filters={filters} sources={sources} onChange={setFilters} />
          </div>
          {shown.length > 0 ? (
            <ResultsGrid
              comics={shown}
              selectedId={mobile ? selectedId : (panelComic?.comicId ?? null)}
              dates={dates}
              onSelect={(comic) => select(comic.comicId)}
            />
          ) : !filters.attentionOnly || attentionCount > 0 ? (
            <p className="border-t border-border p-8 text-center text-ink-subtle">No comics match.</p>
          ) : (
            <p className="flex items-center justify-center gap-2 border-t border-border p-8 text-ink-subtle">
              <CheckCircle className="h-5 w-5 text-success" />
              Every comic is up to date.
            </p>
          )}
        </Card>
        <aside className="hidden md:block">
          <ComicRetrievalPanel comic={panelComic} />
        </aside>
      </div>

      <ComicRetrievalDrawer comic={mobile ? selected : null} onOpenChange={(open) => !open && select(null)} />
    </div>
  );
}

function PageSkeleton() {
  return (
    <div className="space-y-6">
      <Skeleton className="h-8 w-48" />
      <Skeleton className="h-40 w-full" />
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {[...Array(3)].map((_, i) => (
          <Skeleton key={i} className="h-24" />
        ))}
      </div>
      <Skeleton className="h-64 w-full" />
    </div>
  );
}

export default function RetrievalStatusPage() {
  return (
    <Suspense fallback={<PageSkeleton />}>
      <RetrievalStatus />
    </Suspense>
  );
}
