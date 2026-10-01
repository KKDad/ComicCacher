'use client';

import { useState } from 'react';
import { AlertTriangle, Search } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { formatMediumDate } from '@/lib/date-utils';
import type { SourceComic } from '@/types/sources';

/** A comic's start, as shown in the catalog: its first strip date, or "#n" for numbered comics. */
export function describeStart(comic: Pick<SourceComic, 'sourceStartDate' | 'firstStripNumber'>, numbered: boolean): string | null {
  if (numbered) {
    return comic.firstStripNumber != null ? `#${comic.firstStripNumber}` : null;
  }
  return comic.sourceStartDate ? formatMediumDate(comic.sourceStartDate) : null;
}

interface StartDateDialogProps {
  comic: SourceComic;
  numbered: boolean;
  canDetect: boolean;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSave: (value: { sourceStartDate?: string; firstStripNumber?: number }) => void;
  onDetect: () => void;
}

/**
 * Shows and corrects where a comic starts at its source. Backfill never looks before it. A value
 * set here is kept over what the source reports, until a stored strip proves it wrong.
 */
export function StartDateDialog({ comic, numbered, canDetect, open, onOpenChange, onSave, onDetect }: StartDateDialogProps) {
  const current = numbered ? (comic.firstStripNumber != null ? String(comic.firstStripNumber) : '') : (comic.sourceStartDate ?? '');
  const [value, setValue] = useState(current);
  const reported = numbered ? comic.reportedStartStripNumber : comic.reportedStartDate;
  const reportedText = numbered ? (reported != null ? `#${reported}` : null) : reported ? formatMediumDate(String(reported)) : null;
  const reportedDiffers = reported != null && String(reported) !== current;
  const laterThanStored = !numbered && !!value && !!comic.oldest && value > comic.oldest;
  const valid = numbered ? /^[1-9]\d*$/.test(value) : /^\d{4}-\d{2}-\d{2}$/.test(value);

  const save = (raw: string) => onSave(numbered ? { firstStripNumber: parseInt(raw, 10) } : { sourceStartDate: raw });

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        if (next) setValue(current);
        onOpenChange(next);
      }}
    >
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Where {comic.name} starts</DialogTitle>
          <DialogDescription>
            {numbered ? 'The first strip number' : 'The first strip date'} the source has. Backfill never looks before it.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <div className="flex flex-wrap items-center gap-2 text-sm">
            <span className="text-muted-foreground">Now:</span>
            <span className="font-medium">{describeStart(comic, numbered) ?? 'Unknown'}</span>
            {comic.startSource && (
              <Badge variant="secondary">{comic.startSource === 'MANUAL' ? 'Set by an admin' : 'From the source'}</Badge>
            )}
          </div>

          {reportedDiffers && reportedText && (
            <div className="flex flex-wrap items-center gap-2 rounded-md border p-2 text-sm">
              <span>
                The source says <span className="font-medium">{reportedText}</span>
              </span>
              <Button size="xs" variant="outline" onClick={() => save(String(reported))}>
                Use this
              </Button>
            </div>
          )}

          <div className="space-y-1">
            <Label htmlFor="start-value">{numbered ? 'First strip number' : 'First strip date'}</Label>
            <Input
              id="start-value"
              type={numbered ? 'number' : 'date'}
              min={numbered ? 1 : undefined}
              value={value}
              onChange={(e) => setValue(e.target.value)}
            />
          </div>

          {laterThanStored && comic.oldest && (
            <p className="flex items-start gap-1 text-sm text-warning">
              <AlertTriangle className="mt-0.5 size-4 shrink-0" aria-hidden />
              Strips from {formatMediumDate(comic.oldest)} are already stored, so the start will be moved back to that date.
            </p>
          )}
        </div>

        <DialogFooter className="gap-2 sm:justify-between">
          {canDetect ? (
            <Button variant="outline" onClick={onDetect} disabled={comic.startPending}>
              <Search aria-hidden /> {comic.startPending ? 'Checking…' : 'Ask the source'}
            </Button>
          ) : (
            <span />
          )}
          <Button onClick={() => save(value)} disabled={!valid || value === current}>
            Save
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
