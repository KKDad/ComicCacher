'use client';

import { useState } from 'react';
import { Calendar } from '@/components/ui/calendar';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { Button } from '@/components/ui/button';
import { CalendarDays } from 'lucide-react';
import { toast } from 'sonner';

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

  const selectedDate = currentDate ? new Date(currentDate + 'T00:00:00') : undefined;
  const fromDate = oldest ? new Date(oldest + 'T00:00:00') : undefined;
  const toDate = newest ? new Date(newest + 'T00:00:00') : undefined;

  const handleSelect = (date: Date | undefined) => {
    if (!date) return;

    const yyyy = date.getFullYear();
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const dd = String(date.getDate()).padStart(2, '0');
    const dateStr = `${yyyy}-${mm}-${dd}`;

    onSelectDate(dateStr);
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
          <CalendarDays className="h-4 w-4" />
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-auto p-0 bg-card border-border" align="center">
        <Calendar
          mode="single"
          selected={selectedDate}
          onSelect={handleSelect}
          defaultMonth={selectedDate}
          startMonth={fromDate}
          endMonth={toDate}
          className="text-ink"
        />
      </PopoverContent>
    </Popover>
  );
}
