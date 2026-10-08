import Link from 'next/link';
import { CheckCircle } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { formatShortDate } from '@/lib/date-utils';
import { StatusBadge } from './status-badge';
import type { TodaysError } from './health';

/** Every failed attempt today, newest first: the raw view, including failures a later attempt fixed. */
export function TodaysErrors({ errors }: { errors: TodaysError[] }) {
  return (
    <Card>
      <div className="p-6 pb-4">
        <h2 className="text-lg font-semibold text-ink">Today’s errors</h2>
        <p className="text-sm text-ink-subtle">Failed attempts made today, newest first, including backfills of older strips.</p>
      </div>
      {errors.length === 0 ? (
        <p className="flex items-center gap-2 border-t border-border px-6 py-4 text-sm text-ink-subtle">
          <CheckCircle className="h-4 w-4 text-success" />
          No errors today.
        </p>
      ) : (
        <ul className="divide-y divide-border border-t border-border">
          {errors.map(({ record, recovered }) => (
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
              {record.errorMessage && <p className="break-words text-xs text-ink-subtle">{record.errorMessage}</p>}
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
}
