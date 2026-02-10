'use client';

import { useAuth } from '@/hooks/use-auth';
import { PageHeader } from '@/components/dashboard/page-header';
import { ContinueReading } from '@/components/dashboard/continue-reading';
import { FavoritesSection } from '@/components/dashboard/favorites-section';
import { TodaysComics } from '@/components/dashboard/todays-comics';

export default function DashboardPage() {
  const { user } = useAuth();

  return (
    <div className="space-y-8">
      <PageHeader displayName={user?.displayName || 'there'} />
      <ContinueReading />
      <FavoritesSection />
      <TodaysComics />
    </div>
  );
}
