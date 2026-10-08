'use client';

import { useState } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { DayOutcome } from '@/generated/graphql';
import { formatDuration, formatFullDate, formatShortDate } from '@/lib/date-utils';
import { formatBytes } from '@/lib/format';
import { StatusBadge } from './status-badge';
import { describeDay, type ComicHealth } from './health';

/** "gocomics · numbered strips · inactive · hidden from readers" */
export function describeComic(comic: ComicHealth): string {
  return [
    comic.source ?? 'no source',
    comic.indexed && 'numbered strips',
    !comic.active && 'inactive',
    !comic.enabled && 'hidden from readers',
  ]
    .filter(Boolean)
    .join(' · ');
}

/** Days listed before "Show all", so the panel fits beside the grid without scrolling on its own. */
export const DAYS_SHOWN = 6;
/** An error longer than this is cut to three lines until "Show full error". */
const LONG_ERROR = 160;

/**
 * A comic's schedule, links, and every day in the window that has a record or a missing strip, newest first. Kept short (six
 * days, errors cut to three lines) so the panel needs no scrollbar; render it with key={comicId} so a new comic starts folded.
 */
export function ComicRetrievalDetail({ comic }: { comic: ComicHealth }) {
  const [allDays, setAllDays] = useState(false);
  const [openErrors, setOpenErrors] = useState<ReadonlySet<string>>(new Set());
  const days = comic.days.filter((d) => d.record != null || d.outcome === DayOutcome.Missing).toReversed();
  const visible = allDays ? days : days.slice(0, DAYS_SHOWN);
  const toggleError = (date: string) =>
    setOpenErrors((open) => {
      const next = new Set(open);
      if (!next.delete(date)) next.add(date);
      return next;
    });

  return (
    // min-w-0 and wrap-anywhere: an error with a long URL or token wraps instead of widening the panel
    <div className="min-w-0 space-y-6">
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
            {visible.map((day) => (
              <li key={day.date} className="space-y-1 p-3 text-sm">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="font-medium text-ink">{formatShortDate(day.date)}</span>
                  {day.record && <StatusBadge status={day.record.status} />}
                  <span className="min-w-0 wrap-anywhere text-ink-subtle">{describeDay(day)}</span>
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
                      <ErrorText
                        text={day.record.errorMessage}
                        open={openErrors.has(day.date)}
                        onToggle={() => toggleError(day.date)}
                      />
                    )}
                  </>
                )}
              </li>
            ))}
          </ul>
        )}
        {days.length > DAYS_SHOWN && (
          <Button variant="ghost" size="sm" className="mt-2" onClick={() => setAllDays((a) => !a)}>
            {allDays ? 'Show fewer' : `Show all ${days.length}`}
          </Button>
        )}
      </section>
    </div>
  );
}

function ErrorText({ text, open, onToggle }: { text: string; open: boolean; onToggle: () => void }) {
  const long = text.length > LONG_ERROR;
  return (
    <div>
      <p className={`font-mono text-xs wrap-anywhere text-ink-subtle select-all ${long && !open ? 'line-clamp-3' : ''}`}>{text}</p>
      {long && (
        <button type="button" className="text-xs text-ink-muted underline-offset-2 hover:underline" onClick={onToggle}>
          {open ? 'Show less' : 'Show full error'}
        </button>
      )}
    </div>
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
