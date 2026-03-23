import type { LucideIcon } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';

interface EmptyStateProps {
  icon: LucideIcon;
  title: string;
  description: string;
  actionLabel?: string;
  onAction?: () => void;
  actionHref?: string;
}

export function EmptyState({ icon: Icon, title, description, actionLabel, onAction, actionHref }: EmptyStateProps) {
  const button = actionLabel ? (
    actionHref ? (
      <a href={actionHref}>
        <Button>{actionLabel}</Button>
      </a>
    ) : (
      <Button onClick={onAction}>{actionLabel}</Button>
    )
  ) : null;

  return (
    <Card className="border-dashed">
      <div className="flex flex-col items-center justify-center p-12 text-center">
        <Icon className="h-12 w-12 text-ink-muted mb-4" />
        <p className="text-ink-subtle mb-2">{title}</p>
        <p className="text-sm text-ink-muted mb-4">{description}</p>
        {button}
      </div>
    </Card>
  );
}
