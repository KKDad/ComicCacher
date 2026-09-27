import { Heart } from 'lucide-react';
import { EmptyState } from '@/components/ui/empty-state';

/** Shown by the Daily Reader when there are no comics to read for the day. */
export function TodayEmptyState() {
  return (
    <div className="py-12">
      <EmptyState
        icon={Heart}
        title="Nothing to read here yet"
        description="The Daily Reader shows your favorite comics. Tap the heart on a comic to add it."
        actionLabel="Pick favorites"
        actionHref="/comics"
      />
    </div>
  );
}
