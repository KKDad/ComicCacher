import { PageHeader } from '@/components/dashboard/page-header';
import { ContinueReading } from '@/components/dashboard/continue-reading';
import { FavoritesSection } from '@/components/dashboard/favorites-section';
import { TodaysComics } from '@/components/dashboard/todays-comics';

export default function DashboardPage() {
  return (
    <div className="space-y-8">
      <PageHeader displayName="there" />
      <ContinueReading />
      <FavoritesSection />
      <TodaysComics />
    </div>
  );
}
