interface PageHeaderProps {
  displayName: string;
}

export function PageHeader({ displayName }: PageHeaderProps) {
  const getGreeting = () => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 18) return 'Good afternoon';
    return 'Good evening';
  };

  return (
    <div className="space-y-1">
      <h1 className="text-3xl font-bold text-ink">
        {getGreeting()}, {displayName}!
      </h1>
      <p className="text-ink-subtle">
        Here's what's happening with your comics today
      </p>
    </div>
  );
}
