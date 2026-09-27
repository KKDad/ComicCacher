'use client';

import Link from 'next/link';
import { HistoryIllustration } from '@/components/illustrations';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { ImageWithFallback } from '@/components/ui/image-with-fallback';
import { formatMediumDate } from '@/lib/date-utils';

interface RecentRead {
  comic: {
    id: number;
    name: string;
    lastStrip?: { imageUrl?: string | null } | null;
  };
  date: string;
  /** No strips newer than the one read. */
  caughtUp?: boolean;
}

interface ContinueReadingProps {
  reads?: RecentRead[];
  isLoading?: boolean;
}

// Every state shares one row height, so the sections below don't jump when
// reading history loads or first appears.
const ROW = 'grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4 min-h-[8.5rem]';

function Heading() {
  return <h2 className="text-xl font-semibold mb-4 text-ink">Continue Where You Left Off</h2>;
}

function ReadCard({ read }: { read: RecentRead }) {
  const href = `/comics/${read.comic.id}/read?date=${read.date}`;

  return (
    <Card className="relative flex-row items-center gap-4 p-3 hover:shadow-md transition-shadow focus-within:ring-[3px] focus-within:ring-ring/50">
      <div className="w-36 shrink-0 aspect-[3/2] overflow-hidden rounded-md bg-canvas border border-border">
        {/* Strips are wide: keep the start of the strip rather than cropping its middle */}
        <ImageWithFallback
          src={read.comic.lastStrip?.imageUrl}
          alt=""
          fit="contain"
          fallbackText={read.comic.name[0]}
          className="strip-image object-left"
        />
      </div>
      <div className="min-w-0 space-y-1">
        <h3 className="font-sans font-semibold text-ink truncate">
          <Link href={href} className="outline-none after:absolute after:inset-0" aria-label={`Continue reading ${read.comic.name}`}>
            {read.comic.name}
          </Link>
        </h3>
        <p className="text-sm text-ink-subtle">
          {read.caughtUp ? 'All caught up' : `Read up to ${formatMediumDate(read.date)}`}
        </p>
        <p className="text-sm font-semibold text-primary" aria-hidden="true">Continue →</p>
      </div>
    </Card>
  );
}

export function ContinueReading({ reads = [], isLoading = false }: ContinueReadingProps) {
  if (isLoading) {
    return (
      <section aria-busy="true">
        <Heading />
        <div className={ROW}>
          {[...Array(3)].map((_, i) => (
            <Card key={i} className="flex-row items-center gap-4 p-3">
              <Skeleton className="w-36 aspect-[3/2] rounded-md" />
              <div className="flex-1 space-y-2">
                <Skeleton className="h-5 w-3/4" />
                <Skeleton className="h-4 w-1/2" />
              </div>
            </Card>
          ))}
        </div>
      </section>
    );
  }

  if (reads.length === 0) {
    return (
      <section>
        <Heading />
        <Card className="min-h-[8.5rem] border-dashed flex-row items-center justify-center gap-4 p-6 text-center sm:text-left">
          <HistoryIllustration className="h-20 w-auto shrink-0" />
          <div>
            <p className="font-medium text-ink">No recent reading history</p>
            <p className="text-sm text-ink-subtle">Open any comic and your place is saved here</p>
          </div>
          <Button asChild className="shrink-0">
            <Link href="/comics">Browse comics</Link>
          </Button>
        </Card>
      </section>
    );
  }

  return (
    <section>
      <Heading />
      <ul className={ROW}>
        {reads.map((read) => (
          <li key={read.comic.id} className="contents">
            <ReadCard read={read} />
          </li>
        ))}
      </ul>
    </section>
  );
}
