'use client';

import { useGetBatchSchedulersQuery, useGetRecentBatchJobsQuery } from '@/generated/graphql';
import type { BatchJob } from '@/generated/graphql';
import { JobCard } from '@/components/batch-jobs/job-card';
import { Skeleton } from '@/components/ui/skeleton';
import { Card } from '@/components/ui/card';

function SummaryBar({
  schedulerCount,
  pausedCount,
  completedCount,
  failedCount,
  lastFailure,
}: {
  schedulerCount: number;
  pausedCount: number;
  completedCount: number;
  failedCount: number;
  lastFailure?: BatchJob;
}) {
  const activeCount = schedulerCount - pausedCount;

  return (
    <div className="flex flex-wrap gap-4 text-sm text-muted-foreground">
      <span>
        <span className="font-semibold text-foreground">{activeCount}</span> active
      </span>
      {pausedCount > 0 && (
        <span>
          <span className="font-semibold text-foreground">{pausedCount}</span> paused
        </span>
      )}
      <span>
        <span className="font-semibold text-foreground">{completedCount}</span> completed
      </span>
      {failedCount > 0 && (
        <span>
          <span className="font-semibold text-destructive">{failedCount}</span> failed
          {lastFailure?.startTime && (
            <> ({formatTimeAgo(lastFailure.startTime)})</>
          )}
        </span>
      )}
    </div>
  );
}

function formatTimeAgo(dateStr: string): string {
  const date = new Date(dateStr);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const hours = Math.floor(diffMs / 3_600_000);
  const days = Math.floor(hours / 24);

  if (days > 0) return `${days}d ago`;
  if (hours > 0) return `${hours}h ago`;
  const minutes = Math.floor(diffMs / 60_000);
  return `${minutes}m ago`;
}

export default function BatchJobsPage() {
  const { data: schedulersData, isLoading: schedulersLoading } = useGetBatchSchedulersQuery();
  const { data: jobsData, isLoading: jobsLoading } = useGetRecentBatchJobsQuery({ count: 50 });

  const isLoading = schedulersLoading || jobsLoading;
  const schedulers = schedulersData?.batchSchedulers ?? [];
  const recentJobs = jobsData?.recentBatchJobs ?? [];

  const executionsByJob = new Map<string, BatchJob[]>();
  for (const job of recentJobs) {
    const list = executionsByJob.get(job.jobName) ?? [];
    if (list.length < 5) {
      list.push(job as BatchJob);
    }
    executionsByJob.set(job.jobName, list);
  }

  const pausedCount = schedulers.filter((s) => s.paused).length;
  const completedCount = recentJobs.filter((j) => j.status === 'COMPLETED').length;
  const failedJobs = recentJobs.filter((j) => j.status === 'FAILED');
  const lastFailure = failedJobs[0] as BatchJob | undefined;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Batch Jobs</h1>
        <p className="text-muted-foreground mt-1">Monitor and manage scheduled batch job execution</p>
      </div>

      {isLoading ? (
        <>
          <Skeleton className="h-6 w-48" />
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {Array.from({ length: 6 }).map((_, i) => (
              <Card key={i} className="p-6">
                <div className="space-y-3">
                  <Skeleton className="h-5 w-40" />
                  <Skeleton className="h-4 w-56" />
                  <Skeleton className="h-4 w-32" />
                  <Skeleton className="h-8 w-full" />
                </div>
              </Card>
            ))}
          </div>
        </>
      ) : (
        <>
          <SummaryBar
            schedulerCount={schedulers.length}
            pausedCount={pausedCount}
            completedCount={completedCount}
            failedCount={failedJobs.length}
            lastFailure={lastFailure}
          />
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {schedulers.map((scheduler) => (
              <JobCard
                key={scheduler.jobName}
                scheduler={scheduler}
                recentExecutions={executionsByJob.get(scheduler.jobName) ?? []}
              />
            ))}
          </div>
          {schedulers.length === 0 && (
            <div className="text-center py-12 text-muted-foreground">
              No batch job schedulers are currently configured.
            </div>
          )}
        </>
      )}
    </div>
  );
}
