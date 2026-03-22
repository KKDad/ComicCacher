import { Skeleton } from '@/components/ui/skeleton';

interface StripSkeletonProps {
  className?: string;
}

export function StripSkeleton({ className }: StripSkeletonProps) {
  return (
    <div className={className}>
      <Skeleton className="w-full aspect-[3/1] rounded-lg motion-safe:animate-pulse" />
      <Skeleton className="h-4 w-32 mt-2 motion-safe:animate-pulse" />
    </div>
  );
}
