'use client';

import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { Play, FileText, ChevronDown, ChevronUp } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Switch } from '@/components/ui/switch';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { LogViewer } from './log-viewer';
import {
  useTriggerJobMutation,
  useToggleJobSchedulerMutation,
  useGetBatchSchedulersQuery,
  useGetRecentBatchJobsQuery,
} from '@/generated/graphql';
import type { BatchSchedulerInfo, BatchJob, BatchStep } from '@/generated/graphql';

const JOB_LABELS: Record<string, string> = {
  ComicDownloadJob: 'Comic Download',
  ComicBackfillJob: 'Comic Backfill',
  ImageMetadataBackfillJob: 'Image Metadata',
  MetricsArchiveJob: 'Metrics Archive',
  RetrievalRecordPurgeJob: 'Record Purge',
  AvatarBackfillJob: 'Avatar Backfill',
};

function cronToHumanReadable(cron: string, timezone: string): string {
  const parts = cron.split(' ');
  if (parts.length < 6) return cron;

  const minute = parts[1];
  const hour = parts[2];
  const h = parseInt(hour, 10);
  const m = parseInt(minute, 10);
  const period = h >= 12 ? 'PM' : 'AM';
  const displayHour = h === 0 ? 12 : h > 12 ? h - 12 : h;
  const displayMinute = m.toString().padStart(2, '0');
  const tz = timezone === 'America/Toronto' ? 'ET' : timezone;

  return `Daily at ${displayHour}:${displayMinute} ${period} ${tz}`;
}

function formatRelativeTime(dateStr: string): string {
  const date = new Date(dateStr);
  const now = new Date();
  const diffMs = date.getTime() - now.getTime();
  const absDiffMs = Math.abs(diffMs);

  if (absDiffMs < 60_000) return 'just now';

  const minutes = Math.floor(absDiffMs / 60_000);
  const hours = Math.floor(absDiffMs / 3_600_000);

  if (diffMs > 0) {
    if (hours > 0) return `in ${hours}h ${minutes % 60}m`;
    return `in ${minutes}m`;
  }
  if (hours > 0) return `${hours}h ${minutes % 60}m ago`;
  return `${minutes}m ago`;
}

function formatDuration(ms: number): string {
  if (ms < 1000) return `${Math.round(ms)}ms`;
  const seconds = ms / 1000;
  if (seconds < 60) return `${seconds.toFixed(1)}s`;
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = Math.round(seconds % 60);
  return `${minutes}m ${remainingSeconds}s`;
}

function getStatusBadge(status: string, paused: boolean) {
  if (paused) {
    return <Badge variant="secondary">PAUSED</Badge>;
  }
  switch (status) {
    case 'COMPLETED':
      return <Badge className="bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200">COMPLETED</Badge>;
    case 'FAILED':
      return <Badge variant="destructive">FAILED</Badge>;
    case 'STARTED':
    case 'STARTING':
      return <Badge className="bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200">RUNNING</Badge>;
    default:
      return <Badge variant="outline">{status}</Badge>;
  }
}

interface JobCardProps {
  scheduler: BatchSchedulerInfo;
  lastExecution?: BatchJob;
}

export function JobCard({ scheduler, lastExecution }: JobCardProps) {
  const [expanded, setExpanded] = useState(false);
  const [confirmRunOpen, setConfirmRunOpen] = useState(false);
  const [logViewerOpen, setLogViewerOpen] = useState(false);
  const queryClient = useQueryClient();

  const toggleMutation = useToggleJobSchedulerMutation({
    onSuccess: (data) => {
      if (data.toggleJobScheduler.errors.length > 0) {
        toast.error(data.toggleJobScheduler.errors[0].message);
        return;
      }
      toast.success(`${scheduler.jobName} ${scheduler.paused ? 'resumed' : 'paused'}`);
      queryClient.invalidateQueries({ queryKey: useGetBatchSchedulersQuery.getKey() });
    },
    onError: (error: Error) => {
      toast.error(`Failed to toggle scheduler: ${error.message}`);
    },
  });

  const triggerMutation = useTriggerJobMutation({
    onSuccess: (data) => {
      if (data.triggerJob.errors.length > 0) {
        toast.error(data.triggerJob.errors[0].message);
        return;
      }
      toast.success(`${scheduler.jobName} triggered successfully`);
      queryClient.invalidateQueries({ queryKey: useGetRecentBatchJobsQuery.getKey() });
      queryClient.invalidateQueries({ queryKey: useGetBatchSchedulersQuery.getKey() });
    },
    onError: (error: Error) => {
      toast.error(`Failed to trigger job: ${error.message}`);
    },
  });

  const label = JOB_LABELS[scheduler.jobName] ?? scheduler.jobName;
  const lastStatus = lastExecution?.status ?? 'UNKNOWN';

  return (
    <>
      <Card>
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between">
            <CardTitle className="text-base font-semibold">{label}</CardTitle>
            {getStatusBadge(lastStatus, scheduler.paused)}
          </div>
        </CardHeader>
        <CardContent className="space-y-3">
          <div className="text-sm text-muted-foreground space-y-1">
            <div>{cronToHumanReadable(scheduler.cronExpression, scheduler.timezone)}</div>
            {scheduler.nextRunTime && (
              <div>Next run: {formatRelativeTime(scheduler.nextRunTime)}</div>
            )}
            {lastExecution?.startTime && (
              <div>
                Last run: {formatRelativeTime(lastExecution.startTime)}
                {lastExecution.durationMs != null && ` (${formatDuration(lastExecution.durationMs)})`}
              </div>
            )}
          </div>

          <div className="flex items-center justify-between pt-2">
            <div className="flex items-center gap-2">
              <Switch
                checked={!scheduler.paused}
                onCheckedChange={(checked) =>
                  toggleMutation.mutate({ jobName: scheduler.jobName, paused: !checked })
                }
                disabled={toggleMutation.isPending}
              />
              <span className="text-sm text-muted-foreground">
                {scheduler.paused ? 'Paused' : 'Active'}
              </span>
            </div>
            <Button
              size="sm"
              variant="outline"
              onClick={() => setConfirmRunOpen(true)}
              disabled={triggerMutation.isPending}
            >
              <Play className="h-4 w-4 mr-1" />
              Run Now
            </Button>
          </div>

          {lastExecution && (
            <Button
              variant="ghost"
              size="sm"
              className="w-full text-muted-foreground"
              onClick={() => setExpanded(!expanded)}
            >
              {expanded ? <ChevronUp className="h-4 w-4 mr-1" /> : <ChevronDown className="h-4 w-4 mr-1" />}
              {expanded ? 'Hide Details' : 'Show Details'}
            </Button>
          )}

          {expanded && lastExecution && (
            <div className="border-t pt-3 space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-muted-foreground">Exit Code</span>
                <span>{lastExecution.exitCode ?? 'N/A'}</span>
              </div>
              {lastExecution.exitDescription && (
                <div>
                  <span className="text-muted-foreground">Error: </span>
                  <span className="text-destructive">{lastExecution.exitDescription}</span>
                </div>
              )}
              {lastExecution.steps?.map((step: BatchStep) => (
                <div key={step.stepName} className="bg-muted/50 rounded p-2 text-xs space-y-1">
                  <div className="font-medium">{step.stepName}</div>
                  <div className="grid grid-cols-3 gap-1 text-muted-foreground">
                    <span>Read: {step.readCount}</span>
                    <span>Write: {step.writeCount}</span>
                    <span>Skip: {step.skipCount}</span>
                  </div>
                </div>
              ))}
              <Button
                variant="outline"
                size="sm"
                className="w-full"
                onClick={() => setLogViewerOpen(true)}
              >
                <FileText className="h-4 w-4 mr-1" />
                View Logs
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      <Dialog open={confirmRunOpen} onOpenChange={setConfirmRunOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Run {label}?</DialogTitle>
            <DialogDescription>
              This will trigger a manual execution of the {label} job immediately.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setConfirmRunOpen(false)}>
              Cancel
            </Button>
            <Button
              onClick={() => {
                triggerMutation.mutate({ jobName: scheduler.jobName });
                setConfirmRunOpen(false);
              }}
            >
              Run Now
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {lastExecution && (
        <LogViewer
          open={logViewerOpen}
          onOpenChange={setLogViewerOpen}
          executionId={lastExecution.executionId}
          jobName={scheduler.jobName}
          jobLabel={label}
        />
      )}
    </>
  );
}
