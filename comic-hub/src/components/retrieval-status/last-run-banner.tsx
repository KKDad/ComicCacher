'use client';

import { useState } from 'react';
import Link from 'next/link';
import { CalendarClock, FileText } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { LogViewer } from '@/components/batch-jobs/log-viewer';
import { formatDuration, formatFullDate } from '@/lib/date-utils';
import { countDay, type ComicHealth, type RetrievalHealth } from './health';

const runStatusColors: Record<string, string> = {
  COMPLETED: 'bg-success-subtle text-success',
  FAILED: 'bg-error-subtle text-error',
  STARTED: 'bg-warning-subtle text-warning',
  STARTING: 'bg-warning-subtle text-warning',
};

interface LastRunBannerProps {
  targetDate: string;
  lastRun: RetrievalHealth['lastRun'];
  comics: ComicHealth[];
}

/** The latest daily download run, and what today looks like: strips on disk, missing and still to come. */
export function LastRunBanner({ targetDate, lastRun, comics }: LastRunBannerProps) {
  const [logOpen, setLogOpen] = useState(false);
  const today = countDay(comics, targetDate);

  return (
    <Card className="p-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="space-y-1">
          <h2 className="flex items-center gap-2 text-lg font-semibold text-ink">
            <CalendarClock className="h-5 w-5" />
            {formatFullDate(targetDate)}
          </h2>
          {lastRun ? (
            <p className="flex flex-wrap items-center gap-2 text-sm text-ink-subtle">
              <span>Last daily run</span>
              <span
                className={`rounded-full px-2 py-0.5 text-xs font-medium ${runStatusColors[lastRun.status] ?? 'bg-muted text-muted-foreground'}`}
              >
                {lastRun.status}
              </span>
              {lastRun.startTime && <span>started {new Date(lastRun.startTime).toLocaleString()}</span>}
              {lastRun.durationMs != null && <span>· took {formatDuration(lastRun.durationMs)}</span>}
            </p>
          ) : (
            <p className="text-sm text-ink-subtle">The daily download hasn’t run yet.</p>
          )}
        </div>
        <div className="flex gap-2">
          {lastRun && (
            <Button variant="outline" size="sm" onClick={() => setLogOpen(true)}>
              <FileText className="h-4 w-4" />
              View log
            </Button>
          )}
          <Button variant="outline" size="sm" asChild>
            <Link href="/batch-jobs">Batch jobs</Link>
          </Button>
        </div>
      </div>
      <dl className="mt-4 grid grid-cols-3 gap-4 text-center sm:max-w-md">
        <div>
          <dt className="text-xs text-ink-subtle">On disk today</dt>
          <dd className="text-2xl font-bold text-success">{today.onDisk}</dd>
        </div>
        <div>
          <dt className="text-xs text-ink-subtle">Missing</dt>
          <dd className={`text-2xl font-bold ${today.missing > 0 ? 'text-error' : 'text-ink'}`}>{today.missing}</dd>
        </div>
        <div>
          <dt className="text-xs text-ink-subtle">Waiting for the run</dt>
          <dd className="text-2xl font-bold text-ink">{today.pending}</dd>
        </div>
      </dl>
      {lastRun && (
        <LogViewer
          open={logOpen}
          onOpenChange={setLogOpen}
          executionId={lastRun.executionId}
          jobName={lastRun.jobName}
          jobLabel="Daily download"
        />
      )}
    </Card>
  );
}
