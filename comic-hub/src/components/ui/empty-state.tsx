import Link from 'next/link';
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
        <Icon className="h-12 w-12 text-ink-muted mb-4" aria-hidden="true" />
        <p className="text-ink font-medium mb-2">{title}</p>
        <p className="text-sm text-ink-subtle mb-4">{description}</p>
        {button}
      </div>
    </Card>
  );
}
