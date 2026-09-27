import Link from 'next/link';
import type { LucideIcon } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';

interface EmptyStateProps {
  /** A spot illustration from components/illustrations; takes the place of the icon. */
  illustration?: React.ComponentType<{ className?: string }>;
  icon?: LucideIcon;
  title: string;
  description: string;
  actionLabel?: string;
  onAction?: () => void;
  actionHref?: string;
}

export function EmptyState({ illustration: Illustration, icon: Icon, title, description, actionLabel, onAction, actionHref }: EmptyStateProps) {
  const button = actionLabel ? (
    actionHref ? (
      <Button asChild>
        <Link href={actionHref}>{actionLabel}</Link>
      </Button>
    ) : (
      <Button onClick={onAction}>{actionLabel}</Button>
    )
  ) : null;

  return (
    <Card className="border-dashed">
      <div className="flex flex-col items-center justify-center p-12 text-center">
        {Illustration ? (
          <Illustration className="w-40 h-auto mb-4" />
        ) : (
          Icon && <Icon className="h-12 w-12 text-ink-muted mb-4" aria-hidden="true" />
        )}
        <p className="font-heading text-lg font-bold text-ink mb-2">{title}</p>
        <p className="text-sm text-ink-subtle mb-4 max-w-xs">{description}</p>
        {button}
      </div>
    </Card>
  );
}
