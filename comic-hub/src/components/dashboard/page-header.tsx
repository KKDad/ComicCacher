'use client';

import { useSyncExternalStore } from 'react';

interface PageHeaderProps {
  displayName: string;
}

const noSubscribe = () => () => {};

function greetingFor(hour: number): string {
  if (hour < 12) return 'Good morning';
  if (hour < 18) return 'Good afternoon';
  return 'Good evening';
}

export function PageHeader({ displayName }: PageHeaderProps) {
  // The server doesn't know the reader's clock: render a neutral greeting there
  // and during hydration, then the time-of-day one in the browser.
  const greeting = useSyncExternalStore(
    noSubscribe,
    () => greetingFor(new Date().getHours()),
    () => 'Welcome back',
  );

  return (
    <div className="space-y-1">
      <h1 className="text-3xl font-bold text-ink">
        {greeting}, {displayName}!
      </h1>
      <p className="text-ink-subtle">
        Here&apos;s what&apos;s new in your comics
      </p>
    </div>
  );
}
