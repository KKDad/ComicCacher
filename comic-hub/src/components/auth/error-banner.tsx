'use client';

import { AlertCircle, X } from 'lucide-react';
import { cn } from '@/lib/utils';

interface ErrorBannerProps {
  message: string;
  onDismiss?: () => void;
  className?: string;
}

export function ErrorBanner({ message, onDismiss, className }: ErrorBannerProps) {
  return (
    <div
      className={cn(
        'flex items-start gap-3 rounded-lg bg-error-subtle border border-error p-4 animate-shake',
        className
      )}
      role="alert"
    >
      <AlertCircle className="h-5 w-5 text-error flex-shrink-0 mt-0.5" />
      <div className="flex-1 text-sm text-ink">
        {message}
      </div>
      {onDismiss && (
        <button
          type="button"
          onClick={onDismiss}
          className="flex-shrink-0 text-ink-muted hover:text-ink transition-colors"
          aria-label="Dismiss error"
        >
          <X className="h-4 w-4" />
        </button>
      )}
    </div>
  );
}
