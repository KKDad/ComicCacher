'use client';

import { Button } from '@/components/ui/button';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import {
  SkipBack,
  SkipForward,
  Shuffle,
  Loader2,
} from 'lucide-react';

interface ReaderControlsProps {
  onFirst: () => void;
  onLast: () => void;
  onRandom: () => void;
  isLoadingRandom: boolean;
  datePicker?: React.ReactNode;
}

export function ReaderControls({
  onFirst,
  onLast,
  onRandom,
  isLoadingRandom,
  datePicker,
}: ReaderControlsProps) {
  return (
    <TooltipProvider>
      <div className="flex items-center gap-1">
        <Tooltip>
          <TooltipTrigger asChild>
            <Button
              variant="ghost"
              size="icon"
              onClick={onFirst}
              aria-label="Go to first strip"
              className="text-ink-subtle hover:text-ink hover:bg-muted"
            >
              <SkipBack className="h-5 w-5" />
            </Button>
          </TooltipTrigger>
          <TooltipContent side="bottom">First strip</TooltipContent>
        </Tooltip>

        <Tooltip>
          <TooltipTrigger asChild>
            <Button
              variant="ghost"
              size="icon"
              onClick={onRandom}
              disabled={isLoadingRandom}
              aria-label="Go to random strip"
              className="text-ink-subtle hover:text-ink hover:bg-muted"
            >
              {isLoadingRandom ? (
                <Loader2 className="h-5 w-5 animate-spin" />
              ) : (
                <Shuffle className="h-5 w-5" />
              )}
            </Button>
          </TooltipTrigger>
          <TooltipContent side="bottom">Random strip</TooltipContent>
        </Tooltip>

        {datePicker}

        <Tooltip>
          <TooltipTrigger asChild>
            <Button
              variant="ghost"
              size="icon"
              onClick={onLast}
              aria-label="Go to latest strip"
              className="text-ink-subtle hover:text-ink hover:bg-muted"
            >
              <SkipForward className="h-5 w-5" />
            </Button>
          </TooltipTrigger>
          <TooltipContent side="bottom">Latest strip</TooltipContent>
        </Tooltip>
      </div>
    </TooltipProvider>
  );
}
