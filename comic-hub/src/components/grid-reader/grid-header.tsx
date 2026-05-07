'use client';

import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { ArrowLeft, ChevronLeft, ChevronRight } from 'lucide-react';
import { DatePickerPopover } from '@/components/reader/date-picker-popover';
import { formatFullDate } from '@/lib/date-utils';

interface GridHeaderProps {
  date: string;
  onPreviousDate: () => void;
  onNextDate: () => void;
  onSelectDate: (date: string) => void;
  onToday: () => void;
}

export function GridHeader({ date, onPreviousDate, onNextDate, onSelectDate, onToday }: GridHeaderProps) {
  const router = useRouter();

  return (
    <header className="fixed top-0 left-0 right-0 z-sticky h-14 bg-canvas/90 backdrop-blur-sm border-b border-border flex items-center px-4 gap-2">
      <Button
        variant="ghost"
        size="icon"
        onClick={() => router.back()}
        aria-label="Go back"
        className="text-ink-subtle hover:text-ink hover:bg-muted"
      >
        <ArrowLeft className="h-5 w-5" />
      </Button>

      <Button
        variant="ghost"
        size="icon"
        onClick={onPreviousDate}
        aria-label="Previous date"
        className="text-ink-subtle hover:text-ink hover:bg-muted"
      >
        <ChevronLeft className="h-5 w-5" />
      </Button>

      <h1 className="text-sm font-medium text-ink truncate flex-1 text-center">
        {formatFullDate(date)}
      </h1>

      <Button
        variant="ghost"
        size="icon"
        onClick={onNextDate}
        aria-label="Next date"
        className="text-ink-subtle hover:text-ink hover:bg-muted"
      >
        <ChevronRight className="h-5 w-5" />
      </Button>

      <DatePickerPopover
        oldest={null}
        newest={null}
        currentDate={date}
        onSelectDate={onSelectDate}
      />

      <Button
        variant="ghost"
        size="sm"
        onClick={onToday}
        className="text-xs text-ink-subtle hover:text-ink hover:bg-muted"
      >
        Today
      </Button>
    </header>
  );
}
