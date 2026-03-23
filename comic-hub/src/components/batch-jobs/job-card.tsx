'use client';

import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { Play, FileText, ChevronDown, ChevronUp, Info } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Switch } from '@/components/ui/switch';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { LogViewer } from './log-viewer';
import { ParameterControl } from './parameter-control';
import {
  useTriggerJobMutation,
  useToggleJobSchedulerMutation,
  useGetBatchSchedulersQuery,
  useGetRecentBatchJobsQuery,
} from '@/generated/graphql';
import type { BatchSchedulerInfo, BatchJob } from '@/generated/graphql';

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

function formatAbsoluteTime(dateStr: string): string {
  const date = new Date(dateStr);
  return date.toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
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

function getSmallStatusBadge(status: string) {
  switch (status) {
    case 'COMPLETED':
      return <Badge className="bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200 text-xs px-1.5 py-0">COMPLETED</Badge>;
    case 'FAILED':
      return <Badge variant="destructive" className="text-xs px-1.5 py-0">FAILED</Badge>;
    case 'STARTED':
    case 'STARTING':
      return <Badge className="bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200 text-xs px-1.5 py-0">RUNNING</Badge>;
    default:
      return <Badge variant="outline" className="text-xs px-1.5 py-0">{status}</Badge>;
  }
}

function getStatusAccentClass(status: string, paused: boolean): string {
  if (paused) return 'border-l-4 border-l-yellow-500';
  switch (status) {
    case 'COMPLETED':
      return 'border-l-4 border-l-green-500';
    case 'FAILED':
      return 'border-l-4 border-l-red-500';
    case 'STARTED':
    case 'STARTING':
      return 'border-l-4 border-l-blue-500';
    default:
      return '';
  }
}

interface JobCardProps {
  scheduler: BatchSchedulerInfo;
  recentExecutions: BatchJob[];
  onJobTriggered?: () => void;
}

export function JobCard({ scheduler, recentExecutions, onJobTriggered }: JobCardProps) {
  const [expanded, setExpanded] = useState(false);
  const [confirmRunOpen, setConfirmRunOpen] = useState(false);
  const [logViewerExecution, setLogViewerExecution] = useState<BatchJob | null>(null);
  const [paramValues, setParamValues] = useState<Record<string, string>>({});
  const queryClient = useQueryClient();

  const lastExecution = recentExecutions[0] ?? undefined;

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
      onJobTriggered?.();
    },
    onError: (error: Error) => {
      toast.error(`Failed to trigger job: ${error.message}`);
    },
  });

  const label = JOB_LABELS[scheduler.jobName] ?? scheduler.jobName;
  const lastStatus = lastExecution?.status ?? 'UNKNOWN';
  const accentClass = getStatusAccentClass(lastStatus, scheduler.paused);

  return (
    <>
      <Card className={accentClass}>
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between">
            <TooltipProvider>
              <div className="flex items-center gap-2">
                <CardTitle className="text-base font-semibold">{label}</CardTitle>
                {scheduler.description && (
                  <Tooltip>
                    <TooltipTrigger asChild>
                      <Info className="h-4 w-4 text-muted-foreground cursor-help shrink-0" />
                    </TooltipTrigger>
                    <TooltipContent side="bottom">{scheduler.description}</TooltipContent>
                  </Tooltip>
                )}
              </div>
            </TooltipProvider>
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
                Last Ran: {formatAbsoluteTime(lastExecution.startTime)}
                {lastExecution.durationMs != null && ` (${formatDuration(lastExecution.durationMs)})`}
                {' · '}{formatRelativeTime(lastExecution.startTime)}
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
              onClick={() => {
                const defaults: Record<string, string> = {};
                for (const p of scheduler.availableParameters ?? []) {
                  defaults[p.name] = p.defaultValue ?? '';
                }
                setParamValues(defaults);
                setConfirmRunOpen(true);
              }}
              disabled={triggerMutation.isPending}
            >
              <Play className="h-4 w-4 mr-1" />
              Run Now
            </Button>
          </div>

          {recentExecutions.length > 0 && (
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

          {expanded && recentExecutions.length > 0 && (
            <div className="border-t pt-3 space-y-3 text-sm">
              <div className="font-medium text-muted-foreground">Recent Runs</div>
              <div className="divide-y">
                {recentExecutions.map((execution, idx) => (
                  <div key={`${execution.executionId}-${idx}`} className="py-2 first:pt-0 last:pb-0 space-y-1">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        {getSmallStatusBadge(execution.status)}
                        <span className="text-sm text-muted-foreground">
                          {execution.startTime ? formatAbsoluteTime(execution.startTime) : 'Unknown'}
                        </span>
                        {execution.durationMs != null && (
                          <span className="text-xs text-muted-foreground">
                            ({formatDuration(execution.durationMs)})
                          </span>
                        )}
                      </div>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="h-7 text-xs text-muted-foreground"
                        onClick={() => setLogViewerExecution(execution)}
                      >
                        <FileText className="h-3.5 w-3.5 mr-1" />
                        View Logs
                      </Button>
                    </div>
                    {execution.exitDescription && (
                      <div className="text-xs text-destructive">{execution.exitDescription}</div>
                    )}
                  </div>
                ))}
              </div>
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
          {scheduler.availableParameters?.length > 0 && (
            <div className="space-y-4 py-2">
              {scheduler.availableParameters?.map((param) => (
                <ParameterControl
                  key={param.name}
                  param={param}
                  value={paramValues[param.name] ?? param.defaultValue ?? ''}
                  onChange={(value) =>
                    setParamValues((prev) => ({ ...prev, [param.name]: value }))
                  }
                />
              ))}
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setConfirmRunOpen(false)}>
              Cancel
            </Button>
            <Button
              onClick={() => {
                const hasParams = (scheduler.availableParameters?.length ?? 0) > 0;
                triggerMutation.mutate({
                  jobName: scheduler.jobName,
                  parameters: hasParams ? paramValues : undefined,
                });
                setConfirmRunOpen(false);
              }}
            >
              Run Now
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {logViewerExecution && (
        <LogViewer
          open={!!logViewerExecution}
          onOpenChange={(open) => { if (!open) setLogViewerExecution(null); }}
          executionId={logViewerExecution.executionId}
          jobName={scheduler.jobName}
          jobLabel={label}
        />
      )}
    </>
  );
}
