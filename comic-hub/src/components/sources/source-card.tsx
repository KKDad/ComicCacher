'use client';

import { useState } from 'react';
import Link from 'next/link';
import { AlertTriangle, ChevronDown, ChevronUp, History, Library, RefreshCw } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { SourceSettingsList } from './source-settings';
import { formatAbsoluteTime, formatRelativeTime } from '@/lib/date-utils';
import type { SourceSummary } from '@/types/sources';

interface SourceCardProps {
  source: SourceSummary;
  canChange: boolean;
  onRefresh: (sourceId: string) => void;
  onBackfill: (sourceId: string) => void;
}

/** One comic source: how many of its comics are configured, its catalog, and its settings. */
export function SourceCard({ source, canChange, onRefresh, onBackfill }: SourceCardProps) {
  const [showSettings, setShowSettings] = useState(false);
  const [confirmBackfill, setConfirmBackfill] = useState(false);

  return (
    <Card>
      <CardHeader className="pb-2">
        <div className="flex items-start justify-between gap-2">
          <CardTitle className="text-lg">{source.displayName}</CardTitle>
          <Badge variant="outline">{source.kind === 'INDEXED' ? 'Numbered' : 'Daily'}</Badge>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="text-sm">
          {source.hasCatalog ? (
            <p>
              <span className="font-semibold">{source.configuredCount}</span> of{' '}
              <span className="font-semibold">{source.catalogCount}</span> comics configured
              <span className="text-muted-foreground"> · {source.activeCount} downloading</span>
            </p>
          ) : (
            <p>
              <span className="font-semibold">{source.configuredCount}</span> comics configured
            </p>
          )}
          {source.hasCatalog && (
            <p className="text-muted-foreground">
              {source.refreshing ? (
                'Reading the catalog now…'
              ) : source.lastRefreshed ? (
                <>
                  Catalog read <time dateTime={source.lastRefreshed} title={formatAbsoluteTime(source.lastRefreshed)}>{formatRelativeTime(source.lastRefreshed)}</time>
                </>
              ) : (
                'Catalog not read yet'
              )}
            </p>
          )}
          {source.lastRefreshError && !source.refreshing && (
            <p className="mt-1 flex items-start gap-1 text-destructive">
              <AlertTriangle className="mt-0.5 size-4 shrink-0" aria-hidden />
              <span className="break-words">Last refresh failed: {source.lastRefreshError}</span>
            </p>
          )}
        </div>

        <div className="flex flex-wrap gap-2">
          <Button asChild size="sm">
            <Link href={`/sources/${source.id}`}>
              <Library aria-hidden /> Browse comics
            </Link>
          </Button>
          {canChange && source.hasCatalog && (
            <Button size="sm" variant="outline" onClick={() => onRefresh(source.id)} disabled={source.refreshing}>
              <RefreshCw className={source.refreshing ? 'animate-spin' : undefined} aria-hidden /> Refresh catalog
            </Button>
          )}
          {canChange && (
            <Button size="sm" variant="outline" onClick={() => setConfirmBackfill(true)} disabled={source.configuredCount === 0}>
              <History aria-hidden /> Backfill
            </Button>
          )}
        </div>

        <div>
          <button
            type="button"
            className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
            onClick={() => setShowSettings((s) => !s)}
            aria-expanded={showSettings}
          >
            {showSettings ? <ChevronUp className="size-4" aria-hidden /> : <ChevronDown className="size-4" aria-hidden />}
            Settings
          </button>
          {showSettings && (
            <div className="mt-2">
              <SourceSettingsList settings={source.settings} />
            </div>
          )}
        </div>
      </CardContent>

      <Dialog open={confirmBackfill} onOpenChange={setConfirmBackfill}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Backfill {source.displayName}?</DialogTitle>
            <DialogDescription>
              Downloads missing strips for every {source.displayName} comic that is downloading, within the source&apos;s backfill limits.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setConfirmBackfill(false)}>
              Cancel
            </Button>
            <Button
              onClick={() => {
                onBackfill(source.id);
                setConfirmBackfill(false);
              }}
            >
              Start backfill
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </Card>
  );
}
