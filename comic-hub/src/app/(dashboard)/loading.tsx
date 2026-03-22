import { Skeleton } from '@/components/ui/skeleton';

export default function DashboardLoading() {
  return (
    <div className="p-6 space-y-8">
      <div>
        <Skeleton className="h-8 w-64 mb-2 bg-muted" />
        <Skeleton className="h-4 w-80 bg-muted" />
      </div>

      <div>
        <Skeleton className="h-6 w-32 mb-4 bg-muted" />
        <div className="flex gap-4">
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-20 w-20 rounded-full bg-muted" />
          ))}
        </div>
      </div>

      <div>
        <Skeleton className="h-6 w-40 mb-4 bg-muted" />
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="rounded-lg overflow-hidden">
              <Skeleton className="w-full aspect-[3/1] bg-muted" />
              <div className="p-3">
                <Skeleton className="h-4 w-24 mb-1 bg-muted" />
                <Skeleton className="h-3 w-16 bg-muted" />
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
