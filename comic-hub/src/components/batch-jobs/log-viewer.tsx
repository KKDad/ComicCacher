'use client';

import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from '@/components/ui/sheet';
import { Skeleton } from '@/components/ui/skeleton';
import { useGetBatchJobLogQuery } from '@/generated/graphql';

interface LogViewerProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  executionId: number;
  jobName: string;
  jobLabel: string;
}

export function LogViewer({ open, onOpenChange, executionId, jobName, jobLabel }: LogViewerProps) {
  const { data, isLoading } = useGetBatchJobLogQuery(
    { executionId, jobName },
    { enabled: open }
  );

  const logContent = data?.batchJobLog;

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="w-full sm:max-w-2xl overflow-hidden flex flex-col">
        <SheetHeader>
          <SheetTitle>{jobLabel} - Execution #{executionId}</SheetTitle>
          <SheetDescription>Log output for this job execution</SheetDescription>
        </SheetHeader>
        <div className="flex-1 overflow-auto mt-4">
          {isLoading && (
            <div className="space-y-2">
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-3/4" />
              <Skeleton className="h-4 w-5/6" />
              <Skeleton className="h-4 w-2/3" />
              <Skeleton className="h-4 w-full" />
            </div>
          )}
          {!isLoading && !logContent && (
            <div className="flex items-center justify-center h-32 text-muted-foreground">
              No logs available for this execution
            </div>
          )}
          {!isLoading && logContent && (
            <pre className="bg-zinc-950 text-zinc-100 rounded-md p-4 text-xs font-mono leading-relaxed whitespace-pre-wrap break-all overflow-auto max-h-[calc(100vh-12rem)]">
              {logContent}
            </pre>
          )}
        </div>
      </SheetContent>
    </Sheet>
  );
}
