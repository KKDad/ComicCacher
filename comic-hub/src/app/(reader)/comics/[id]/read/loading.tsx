import { Skeleton } from '@/components/ui/skeleton';

export default function ReaderLoading() {
  return (
    <div className="min-h-screen bg-canvas">
      <div className="h-14 border-b border-border bg-canvas/90 flex items-center px-4">
        <Skeleton className="h-5 w-5 rounded bg-muted" />
        <Skeleton className="h-5 w-40 ml-3 bg-muted" />
      </div>

      <main className="pt-4 pb-8 px-4">
        <div className="max-w-3xl mx-auto space-y-6">
          {[1, 2, 3].map((i) => (
            <div key={i} className="py-4">
              <Skeleton className="h-4 w-40 mb-2 bg-muted" />
              <Skeleton className="w-full aspect-[3/1] rounded-lg bg-muted" />
            </div>
          ))}
        </div>
      </main>
    </div>
  );
}
