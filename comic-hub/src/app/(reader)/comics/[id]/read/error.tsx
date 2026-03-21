'use client';

import { Button } from '@/components/ui/button';

export default function ReaderError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <div className="min-h-screen bg-canvas flex flex-col items-center justify-center gap-4 p-4 text-center">
      <h2 className="text-lg font-medium text-ink">Something went wrong</h2>
      <p className="text-sm text-ink-subtle max-w-md">
        The reader encountered an error. Try again or go back to the dashboard.
      </p>
      <div className="flex gap-3">
        <Button variant="outline" onClick={() => window.history.back()}>
          Go Back
        </Button>
        <Button onClick={reset}>Try Again</Button>
      </div>
    </div>
  );
}
