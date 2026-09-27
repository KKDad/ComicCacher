'use client';

import { useState } from 'react';
import { Calendar } from '@/components/ui/calendar';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { Button } from '@/components/ui/button';
import { CalendarDays } from 'lucide-react';
import { parseDate, toIsoDate } from '@/lib/date-utils';

interface DatePickerPopoverProps {
  oldest: string | null;
  newest: string | null;
  currentDate: string | null;
  onSelectDate: (date: string) => void;
}

export function DatePickerPopover({
  oldest,
  newest,
  currentDate,
  onSelectDate,
}: DatePickerPopoverProps) {
  const [open, setOpen] = useState(false);

  const selectedDate = currentDate ? parseDate(currentDate) : undefined;
  const fromDate = oldest ? parseDate(oldest) : undefined;
  const toDate = newest ? parseDate(newest) : undefined;

  const handleSelect = (date: Date | undefined) => {
    if (!date) return;

    onSelectDate(toIsoDate(date));
    setOpen(false);
  };

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="ghost"
          size="icon"
          aria-label="Pick a date"
          className="text-ink-subtle hover:text-ink hover:bg-muted"
        >
          <CalendarDays className="h-5 w-5" />
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-auto p-0 bg-card border-border" align="center">
        <Calendar
          mode="single"
          captionLayout="dropdown"
          selected={selectedDate}
          onSelect={handleSelect}
          defaultMonth={selectedDate}
          startMonth={fromDate}
          endMonth={toDate}
          disabled={[
            ...(fromDate ? [{ before: fromDate }] : []),
            ...(toDate ? [{ after: toDate }] : []),
          ]}
          className="text-ink"
        />
      </PopoverContent>
    </Popover>
  );
}
