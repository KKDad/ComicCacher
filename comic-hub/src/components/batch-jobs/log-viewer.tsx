'use client';

import { useState, useCallback, useEffect, useRef, useMemo } from 'react';
import { ChevronUp, ChevronDown, Search } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Skeleton } from '@/components/ui/skeleton';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useGetBatchJobLogQuery } from '@/generated/graphql';

interface LogViewerProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  executionId: number;
  jobName: string;
  jobLabel: string;
}

const LOG_LINE_REGEX = /^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}\s+\w+\s+\[.*?]\s+)(\S+)(\s+:.*)$/;

function alignLoggerColumn(content: string): string {
  const lines = content.split('\n');
  let maxLoggerLen = 0;

  const parsed = lines.map((line) => {
    const match = line.match(LOG_LINE_REGEX);
    if (match) {
      maxLoggerLen = Math.max(maxLoggerLen, match[2].length);
      return { prefix: match[1], logger: match[2], suffix: match[3] };
    }
    return null;
  });

  return parsed
    .map((p, i) =>
      p ? `${p.prefix}${p.logger.padEnd(maxLoggerLen)}${p.suffix}` : lines[i],
    )
    .join('\n');
}

function findAllMatches(text: string, query: string): number[] {
  const indices: number[] = [];
  const lowerText = text.toLowerCase();
  const lowerQuery = query.toLowerCase();
  let pos = 0;
  while (pos < lowerText.length) {
    const idx = lowerText.indexOf(lowerQuery, pos);
    if (idx === -1) break;
    indices.push(idx);
    pos = idx + 1;
  }
  return indices;
}

function HighlightedLog({
  content,
  searchQuery,
  matchIndices,
  activeMatchIndex,
}: {
  content: string;
  searchQuery: string;
  matchIndices: number[];
  activeMatchIndex: number;
}) {
  if (!searchQuery || matchIndices.length === 0) {
    return <>{content}</>;
  }

  const parts: React.ReactNode[] = [];
  let lastEnd = 0;
  const queryLen = searchQuery.length;

  for (let i = 0; i < matchIndices.length; i++) {
    const start = matchIndices[i];
    if (start > lastEnd) {
      parts.push(content.slice(lastEnd, start));
    }
    parts.push(
      <mark
        key={i}
        data-match-index={i}
        className={i === activeMatchIndex ? 'bg-amber-400 text-black' : 'bg-yellow-200/60 text-inherit'}
      >
        {content.slice(start, start + queryLen)}
      </mark>,
    );
    lastEnd = start + queryLen;
  }
  if (lastEnd < content.length) {
    parts.push(content.slice(lastEnd));
  }
  return <>{parts}</>;
}

export function LogViewer({ open, onOpenChange, executionId, jobName, jobLabel }: LogViewerProps) {
  const { data, isLoading } = useGetBatchJobLogQuery(
    { executionId, jobName },
    { enabled: open },
  );

  const logContent = useMemo(
    () => (data?.batchJobLog ? alignLoggerColumn(data.batchJobLog) : undefined),
    [data?.batchJobLog],
  );
  const [searchQuery, setSearchQuery] = useState('');
  const [activeMatchIndex, setActiveMatchIndex] = useState(0);
  const searchInputRef = useRef<HTMLInputElement>(null);
  const preRef = useRef<HTMLPreElement>(null);

  const matchIndices = useMemo(
    () => (searchQuery.length > 0 && logContent ? findAllMatches(logContent, searchQuery) : []),
    [logContent, searchQuery],
  );

  // Reset active match when search changes
  useEffect(() => {
    setActiveMatchIndex(0);
  }, [searchQuery]);

  // Reset search when dialog closes
  useEffect(() => {
    if (!open) {
      setSearchQuery('');
      setActiveMatchIndex(0);
    }
  }, [open]);

  // Scroll to active match
  useEffect(() => {
    if (matchIndices.length === 0 || !preRef.current) return;
    const mark = preRef.current.querySelector(`[data-match-index="${activeMatchIndex}"]`);
    mark?.scrollIntoView?.({ block: 'center', behavior: 'smooth' });
  }, [activeMatchIndex, matchIndices]);

  const goToNextMatch = useCallback(() => {
    if (matchIndices.length === 0) return;
    setActiveMatchIndex((prev) => (prev + 1) % matchIndices.length);
  }, [matchIndices.length]);

  const goToPrevMatch = useCallback(() => {
    if (matchIndices.length === 0) return;
    setActiveMatchIndex((prev) => (prev - 1 + matchIndices.length) % matchIndices.length);
  }, [matchIndices.length]);

  // Intercept Ctrl+F / Cmd+F within the dialog
  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'f') {
        e.preventDefault();
        searchInputRef.current?.focus();
        searchInputRef.current?.select();
      }
      if (e.key === 'Enter' && document.activeElement === searchInputRef.current) {
        e.preventDefault();
        if (e.shiftKey) {
          goToPrevMatch();
        } else {
          goToNextMatch();
        }
      }
    },
    [goToNextMatch, goToPrevMatch],
  );

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        className="max-w-[1920px] w-[95vw] h-[90vh] overflow-hidden flex flex-col"
        onKeyDown={handleKeyDown}
      >
        <DialogHeader>
          <DialogTitle>{jobLabel} - Execution #{executionId}</DialogTitle>
          <DialogDescription>Log output for this job execution</DialogDescription>
        </DialogHeader>
        <div className="flex-1 overflow-auto flex flex-col gap-2">
          {!isLoading && logContent && (
            <div className="flex items-center gap-2">
              <div className="relative flex-1">
                <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground" />
                <Input
                  ref={searchInputRef}
                  placeholder="Search logs..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-8 h-8 text-sm"
                  aria-label="Search logs"
                />
              </div>
              {searchQuery && (
                <>
                  <span className="text-xs text-muted-foreground whitespace-nowrap" data-testid="match-count">
                    {matchIndices.length > 0 ? `${activeMatchIndex + 1} of ${matchIndices.length}` : '0 results'}
                  </span>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-8 w-8"
                    onClick={goToPrevMatch}
                    disabled={matchIndices.length === 0}
                    aria-label="Previous match"
                  >
                    <ChevronUp className="h-4 w-4" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-8 w-8"
                    onClick={goToNextMatch}
                    disabled={matchIndices.length === 0}
                    aria-label="Next match"
                  >
                    <ChevronDown className="h-4 w-4" />
                  </Button>
                </>
              )}
            </div>
          )}
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
            <pre
              ref={preRef}
              className="bg-zinc-950 text-zinc-100 rounded-md p-4 text-xs font-mono leading-relaxed whitespace-pre overflow-auto flex-1"
            >
              <HighlightedLog
                content={logContent}
                searchQuery={searchQuery}
                matchIndices={matchIndices}
                activeMatchIndex={activeMatchIndex}
              />
            </pre>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
