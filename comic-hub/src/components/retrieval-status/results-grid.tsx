'use client';

import { useEffect, useRef } from 'react';
import Link from 'next/link';
import { Check, X } from 'lucide-react';
import { DayOutcome } from '@/generated/graphql';
import { formatShortDate, parseDate } from '@/lib/date-utils';
import { describeDay, type ComicHealth, type RetrievalDay } from './health';

/** Width of the comic and newest columns, and the narrowest a day column may get, in rem. */
const FIXED_COLUMNS_REM = 17;
const MIN_DAY_REM = 1.25;

interface ResultsGridProps {
  comics: ComicHealth[];
  /** The comic shown in the detail panel or sheet, highlighted and kept in view. */
  selectedId?: number | null;
  /** The days to show, oldest first. */
  dates: string[];
  onSelect: (comic: ComicHealth) => void;
  /** Up and down while the grid has focus: move the selection to the previous or next comic. */
  onStep: (delta: 1 | -1) => void;
}

/**
 * One row per comic, one column per day: ✓ the strip is on disk, ✗ an expected strip is missing, blank when none was due.
 * The files decide, so a failure that a later attempt fixed is a ✓ (outlined). The day columns share the card's width; only
 * below the table's minimum width (phones) does it scroll sideways inside its card, opening on the newest day.
 */
export function ResultsGrid({ comics, selectedId = null, dates, onSelect, onStep }: ResultsGridProps) {
  const scroller = useRef<HTMLDivElement>(null);
  const selectedRow = useRef<HTMLTableRowElement>(null);

  useEffect(() => {
    selectedRow.current?.scrollIntoView?.({ block: 'nearest' });
  }, [selectedId]);

  useEffect(() => {
    const el = scroller.current;
    if (el) el.scrollLeft = el.scrollWidth;
  }, [dates.length]);

  return (
    // Focusable so the arrow keys step through the comics while it has focus, and only then; a click on a row focuses it
    <div
      ref={scroller}
      tabIndex={0}
      role="region"
      aria-label="Results by day. Up and down choose a comic."
      className="overflow-x-auto overflow-y-hidden focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ink"
      onKeyDown={(e) => {
        if (e.altKey || e.ctrlKey || e.metaKey || (e.key !== 'ArrowDown' && e.key !== 'ArrowUp')) return;
        e.preventDefault();
        onStep(e.key === 'ArrowDown' ? 1 : -1);
      }}
    >
      <table
        className="w-full table-fixed border-separate border-spacing-0 text-sm"
        style={{ minWidth: `${FIXED_COLUMNS_REM + dates.length * MIN_DAY_REM}rem` }}
      >
        <colgroup>
          <col className="w-40 sm:w-48" />
          {dates.map((date) => (
            <col key={date} />
          ))}
          <col className="w-20" />
        </colgroup>
        <thead>
          <tr className="text-left text-ink-subtle">
            <th className="sticky left-0 z-base border-t border-border bg-card px-4 py-2 font-medium">Comic</th>
            {dates.map((date) => {
              const day = parseDate(date);
              return (
                <th key={date} className="border-t border-border px-0 py-2 text-center text-xs font-normal" title={formatShortDate(date)}>
                  <span className="block text-ink-muted">{day.toLocaleDateString('en-US', { weekday: 'narrow' })}</span>
                  <span className="block tabular-nums">{day.getDate()}</span>
                </th>
              );
            })}
            <th className="border-t border-border px-2 py-2 text-right font-medium whitespace-nowrap">Newest</th>
          </tr>
        </thead>
        <tbody>
          {comics.map((comic) => {
            const days = new Map(comic.days.map((d) => [d.date, d]));
            const selected = comic.comicId === selectedId;
            const rowBg = selected ? 'bg-surface-muted' : 'group-hover:bg-surface-muted';
            return (
              <tr
                key={comic.comicId}
                ref={selected ? selectedRow : undefined}
                aria-current={selected || undefined}
                className="group cursor-pointer"
                onClick={() => onSelect(comic)}
              >
                <th
                  scope="row"
                  className={`sticky left-0 z-base border-t border-border bg-card px-4 py-1.5 text-left font-normal ${rowBg} ${
                    selected ? 'shadow-[inset_3px_0_0_var(--color-ink)]' : ''
                  }`}
                >
                  <span className="flex min-w-0 items-baseline gap-2">
                    <Link
                      href={`/comics/${comic.comicId}/read`}
                      title={comic.comicName}
                      className={`truncate text-ink hover:underline ${selected ? 'font-semibold' : ''}`}
                      onClick={(e) => e.stopPropagation()}
                    >
                      {comic.comicName}
                    </Link>
                    {!comic.active && <span className="shrink-0 text-xs text-ink-muted">inactive</span>}
                    {comic.stale && <span className="shrink-0 text-xs text-error">stale</span>}
                  </span>
                </th>
                {dates.map((date) => (
                  <td key={date} className={`border-t border-border p-0 text-center ${rowBg}`}>
                    <DayCell comicName={comic.comicName} day={days.get(date)} />
                  </td>
                ))}
                <td className={`border-t border-border px-2 py-1.5 text-right whitespace-nowrap text-ink-subtle ${rowBg}`}>
                  {comic.newest ? formatShortDate(comic.newest) : '—'}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

function DayCell({ comicName, day }: { comicName: string; day: RetrievalDay | undefined }) {
  if (!day) return null;
  const label = `${comicName}, ${formatShortDate(day.date)}: ${describeDay(day)}`;

  switch (day.outcome) {
    case DayOutcome.OnDisk:
      return (
        <span
          role="img"
          aria-label={label}
          title={label}
          className={`mx-auto flex h-6 w-full max-w-6 items-center justify-center rounded text-success ${day.recovered ? 'ring-1 ring-warning ring-inset' : ''}`}
        >
          <Check className="h-3.5 w-3.5" />
        </span>
      );
    case DayOutcome.Missing:
      return (
        <span
          role="img"
          aria-label={label}
          title={label}
          className="mx-auto flex h-6 w-full max-w-6 items-center justify-center rounded bg-error-subtle text-error"
        >
          <X className="h-3.5 w-3.5" />
        </span>
      );
    case DayOutcome.Pending:
      return (
        <span role="img" aria-label={label} title={label} className="mx-auto flex h-6 w-full max-w-6 items-center justify-center text-ink-muted">
          ·
        </span>
      );
    default:
      return <span role="img" aria-label={label} title={label} className="mx-auto block h-6 w-full max-w-6" />;
  }
}
