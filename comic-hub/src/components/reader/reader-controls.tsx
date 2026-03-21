'use client';

import { Button } from '@/components/ui/button';
import {
  ChevronsLeft,
  ChevronsRight,
  Shuffle,
  Loader2,
} from 'lucide-react';

interface ReaderControlsProps {
  onFirst: () => void;
  onLast: () => void;
  onRandom: () => void;
  isLoadingRandom: boolean;
  hasOlder: boolean;
  hasNewer: boolean;
  /** Slot for date picker popover (wired in Phase 6) */
  datePicker?: React.ReactNode;
}

export function ReaderControls({
  onFirst,
  onLast,
  onRandom,
  isLoadingRandom,
  hasOlder,
  hasNewer,
  datePicker,
}: ReaderControlsProps) {
  return (
    <div className="flex items-center gap-1">
      <Button
        variant="ghost"
        size="icon"
        onClick={onFirst}
        disabled={!hasOlder}
        aria-label="Go to first strip"
      >
        <ChevronsLeft className="h-4 w-4" />
      </Button>

      <Button
        variant="ghost"
        size="icon"
        onClick={onRandom}
        disabled={isLoadingRandom}
        aria-label="Go to random strip"
      >
        {isLoadingRandom ? (
          <Loader2 className="h-4 w-4 animate-spin" />
        ) : (
          <Shuffle className="h-4 w-4" />
        )}
      </Button>

      {datePicker}

      <Button
        variant="ghost"
        size="icon"
        onClick={onLast}
        disabled={!hasNewer}
        aria-label="Go to latest strip"
      >
        <ChevronsRight className="h-4 w-4" />
      </Button>
    </div>
  );
}
