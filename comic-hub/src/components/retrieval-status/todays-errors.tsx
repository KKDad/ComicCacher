'use client';

import { useState } from 'react';
import Link from 'next/link';
import { CheckCircle } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { formatShortDate } from '@/lib/date-utils';
import { StatusBadge } from './status-badge';
import type { TodaysError } from './health';

/** How many of today's errors show before "Show all", so a bad day doesn't push the grid off the screen. */
export const ERRORS_SHOWN = 5;

/** Every failed attempt today, newest first: the raw view, including failures a later attempt fixed. */
export function TodaysErrors({ errors }: { errors: TodaysError[] }) {
  const [expanded, setExpanded] = useState(false);
  const visible = expanded ? errors : errors.slice(0, ERRORS_SHOWN);

  return (
    <Card>
      <div className="p-6 pb-4">
        <h2 className="text-lg font-semibold text-ink">Today’s errors</h2>
        <p className="text-sm text-ink-subtle">
          Failed attempts made today, newest first, including backfills of older strips. A strip the source didn’t have isn’t
          counted.
        </p>
      </div>
      {errors.length === 0 ? (
        <p className="flex items-center gap-2 border-t border-border px-6 py-4 text-sm text-ink-subtle">
          <CheckCircle className="h-4 w-4 text-success" />
          No errors today.
        </p>
      ) : (
        <ul className="divide-y divide-border border-t border-border">
          {visible.map(({ record, recovered }) => (
            <li key={`${record.id}-${record.attemptedAt}`} className="flex flex-col gap-1 px-6 py-3 text-sm">
              <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
                <span className="text-ink-muted tabular-nums">
                  {record.attemptedAt ? new Date(record.attemptedAt).toLocaleTimeString() : '—'}
                </span>
                {record.comicId != null ? (
                  <Link href={`/comics/${record.comicId}/read`} className="font-medium text-ink hover:underline">
                    {record.comicName}
                  </Link>
                ) : (
                  <span className="font-medium text-ink">{record.comicName}</span>
                )}
                <span className="text-ink-subtle">{formatShortDate(record.comicDate)}</span>
                <StatusBadge status={record.status} />
                {record.httpStatusCode != null && <span className="text-ink-subtle">HTTP {record.httpStatusCode}</span>}
                {record.source && <span className="text-ink-muted">{record.source}</span>}
                {recovered && (
                  <span className="rounded-full bg-success-subtle px-2 py-0.5 text-xs font-medium text-success">since recovered</span>
                )}
              </div>
              {record.errorMessage && <p className="text-xs wrap-anywhere text-ink-subtle">{record.errorMessage}</p>}
            </li>
          ))}
        </ul>
      )}
      {errors.length > ERRORS_SHOWN && (
        <div className="border-t border-border px-6 py-3">
          <Button variant="ghost" size="sm" onClick={() => setExpanded((e) => !e)}>
            {expanded ? 'Show fewer' : `Show all ${errors.length}`}
          </Button>
        </div>
      )}
    </Card>
  );
}
