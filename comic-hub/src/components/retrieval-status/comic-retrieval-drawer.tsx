'use client';

import Link from 'next/link';
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet';
import { Button } from '@/components/ui/button';
import { DayOutcome } from '@/generated/graphql';
import { formatDuration, formatFullDate, formatShortDate } from '@/lib/date-utils';
import { formatBytes } from '@/lib/format';
import { StatusBadge } from './status-badge';
import { describeDay, type ComicHealth } from './health';

interface ComicRetrievalDrawerProps {
  comic: ComicHealth | null;
  onOpenChange: (open: boolean) => void;
}

/** A comic's schedule and every day in the window that has a record or a missing strip, newest first. */
export function ComicRetrievalDrawer({ comic, onOpenChange }: ComicRetrievalDrawerProps) {
  const days = comic
    ? comic.days.filter((d) => d.record != null || d.outcome === DayOutcome.Missing).toReversed()
    : [];

  return (
    <Sheet open={comic != null} onOpenChange={onOpenChange}>
      <SheetContent className="w-full overflow-y-auto sm:max-w-lg">
        {comic && (
          <>
            <SheetHeader>
              <SheetTitle>{comic.comicName}</SheetTitle>
              <SheetDescription>
                {comic.source ?? 'no source'}
                {comic.indexed ? ' · numbered strips' : ''}
                {comic.active ? '' : ' · inactive'}
                {comic.enabled ? '' : ' · hidden from readers'}
              </SheetDescription>
            </SheetHeader>
            <div className="space-y-6 px-4 pb-6">
              <dl className="grid grid-cols-2 gap-3 text-sm">
                <Fact label="Publishes" value={comic.publicationDays?.length ? comic.publicationDays.map((d) => d.slice(0, 3)).join(' ') : 'Daily'} />
                <Fact label="Newest on disk" value={comic.newest ? formatFullDate(comic.newest) : '—'} />
                <Fact label="Expected by now" value={comic.expectedLatest ? formatFullDate(comic.expectedLatest) : '—'} />
                <Fact label="Missing in a row" value={String(comic.missingStreak)} />
              </dl>
              <div className="flex gap-2">
                <Button variant="outline" size="sm" asChild>
                  <Link href={`/comics/${comic.comicId}/read`}>Read</Link>
                </Button>
                {comic.source && (
                  <Button variant="outline" size="sm" asChild>
                    <Link href={`/sources/${comic.source}`}>Source</Link>
                  </Button>
                )}
              </div>
              <section>
                <h3 className="mb-2 text-sm font-semibold text-ink">Attempts and gaps</h3>
                {days.length === 0 ? (
                  <p className="text-sm text-ink-subtle">No attempts or missing strips in the window.</p>
                ) : (
                  <ul className="divide-y divide-border rounded border border-border">
                    {days.map((day) => (
                      <li key={day.date} className="space-y-1 p-3 text-sm">
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="font-medium text-ink">{formatShortDate(day.date)}</span>
                          {day.record && <StatusBadge status={day.record.status} />}
                          <span className="text-ink-subtle">{describeDay(day)}</span>
                        </div>
                        {day.record && (
                          <>
                            <p className="flex flex-wrap gap-x-3 text-xs text-ink-muted">
                              {day.record.attemptedAt && <span>{new Date(day.record.attemptedAt).toLocaleString()}</span>}
                              {day.record.httpStatusCode != null && <span>HTTP {day.record.httpStatusCode}</span>}
                              {day.record.retrievalDurationMs != null && <span>{formatDuration(day.record.retrievalDurationMs)}</span>}
                              {day.record.imageSize != null && <span>{formatBytes(day.record.imageSize)}</span>}
                            </p>
                            {day.record.errorMessage && (
                              <p className="break-words font-mono text-xs text-ink-subtle select-all">{day.record.errorMessage}</p>
                            )}
                          </>
                        )}
                      </li>
                    ))}
                  </ul>
                )}
              </section>
            </div>
          </>
        )}
      </SheetContent>
    </Sheet>
  );
}

function Fact({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-xs text-ink-subtle">{label}</dt>
      <dd className="text-ink">{value}</dd>
    </div>
  );
}
