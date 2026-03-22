'use client';

import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { ArrowLeft } from 'lucide-react';
import { ReaderControls } from './reader-controls';

interface ReaderHeaderProps {
  comicName: string;
  onFirst: () => void;
  onLast: () => void;
  onRandom: () => void;
  isLoadingRandom: boolean;
  datePicker?: React.ReactNode;
}

export function ReaderHeader({
  comicName,
  onFirst,
  onLast,
  onRandom,
  isLoadingRandom,
  datePicker,
}: ReaderHeaderProps) {
  const router = useRouter();

  return (
    <header className="fixed top-0 left-0 right-0 z-sticky h-14 bg-canvas/90 backdrop-blur-sm border-b border-border flex items-center px-4 gap-3">
      <Button
        variant="ghost"
        size="icon"
        onClick={() => router.back()}
        aria-label="Go back"
        className="text-ink-subtle hover:text-ink hover:bg-muted"
      >
        <ArrowLeft className="h-5 w-5" />
      </Button>

      <h1 className="text-sm font-medium text-ink truncate flex-1">
        {comicName}
      </h1>

      <ReaderControls
        onFirst={onFirst}
        onLast={onLast}
        onRandom={onRandom}
        isLoadingRandom={isLoadingRandom}
        datePicker={datePicker}
      />
    </header>
  );
}
