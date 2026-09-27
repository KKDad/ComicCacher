'use client';

import { Button } from '@/components/ui/button';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import {
  ChevronLeft,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight,
  Maximize2,
  Shuffle,
  Loader2,
} from 'lucide-react';

interface ReaderControlsProps {
  onFirst: () => void;
  onLast: () => void;
  onRandom: () => void;
  isLoadingRandom: boolean;
  onOlder?: () => void;
  onNewer?: () => void;
  canGoOlder?: boolean;
  canGoNewer?: boolean;
  onFullscreen?: () => void;
  datePicker?: React.ReactNode;
  /** The reader lists the newest strip at the top, so the up/down keys swap: see useNewestFirst. */
  newestFirst?: boolean;
}

const iconButton = 'h-11 w-11 text-ink-subtle hover:text-ink hover:bg-muted';

/** An icon button whose tooltip names the action and its keyboard shortcut. */
function ControlButton({
  label,
  shortcut,
  onClick,
  disabled,
  children,
}: {
  label: string;
  shortcut?: string;
  onClick: () => void;
  disabled?: boolean;
  children: React.ReactNode;
}) {
  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <Button
          variant="ghost"
          size="icon"
          onClick={onClick}
          disabled={disabled}
          aria-label={label}
          aria-keyshortcuts={shortcut}
          className={iconButton}
        >
          {children}
        </Button>
      </TooltipTrigger>
      <TooltipContent side="bottom">
        {label}
        {shortcut && <kbd className="ml-2 font-sans text-xs opacity-70">{shortcut}</kbd>}
      </TooltipContent>
    </Tooltip>
  );
}

export function ReaderControls({
  onFirst,
  onLast,
  onRandom,
  isLoadingRandom,
  onOlder,
  onNewer,
  canGoOlder = true,
  canGoNewer = true,
  onFullscreen,
  datePicker,
  newestFirst = false,
}: ReaderControlsProps) {
  // J/K and Home/End move down/up the list, so they follow the scroll order
  const olderKey = newestFirst ? 'J' : 'K';
  const newerKey = newestFirst ? 'K' : 'J';
  const firstKey = newestFirst ? 'End' : 'Home';
  const latestKey = newestFirst ? 'Home' : 'End';

  return (
    <TooltipProvider>
      <div className="flex items-center gap-1">
        {onOlder && onNewer && (
          <div className="flex items-center gap-0.5 rounded-lg border border-border p-0.5 mr-1">
            <Button
              variant="ghost"
              onClick={onOlder}
              disabled={!canGoOlder}
              aria-label="Previous strip"
              aria-keyshortcuts={olderKey}
              title={`Previous strip (${olderKey})`}
              className="h-10 gap-1 pl-1.5 pr-2.5 text-ink hover:bg-muted"
            >
              <ChevronLeft className="h-5 w-5" />
              Prev
            </Button>
            <Button
              variant="ghost"
              onClick={onNewer}
              disabled={!canGoNewer}
              aria-label="Next strip"
              aria-keyshortcuts={newerKey}
              title={`Next strip (${newerKey})`}
              className="h-10 gap-1 pl-2.5 pr-1.5 text-ink hover:bg-muted"
            >
              Next
              <ChevronRight className="h-5 w-5" />
            </Button>
          </div>
        )}

        <ControlButton label="First strip" shortcut={firstKey} onClick={onFirst}>
          <ChevronsLeft className="h-5 w-5" />
        </ControlButton>

        <ControlButton label="Random strip" shortcut="R" onClick={onRandom} disabled={isLoadingRandom}>
          {isLoadingRandom ? <Loader2 className="h-5 w-5 animate-spin" /> : <Shuffle className="h-5 w-5" />}
        </ControlButton>

        {datePicker}

        <ControlButton label="Latest strip" shortcut={latestKey} onClick={onLast}>
          <ChevronsRight className="h-5 w-5" />
        </ControlButton>

        {onFullscreen && (
          <ControlButton label="Fullscreen" shortcut="F" onClick={onFullscreen}>
            <Maximize2 className="h-5 w-5" />
          </ControlButton>
        )}
      </div>
    </TooltipProvider>
  );
}
