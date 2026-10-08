import Link from 'next/link';
import { Card } from '@/components/ui/card';
import type { RetrievalHealth } from './health';

type Source = RetrievalHealth['sources'][number];

/** One card per source with today's results, so a source in trouble reads as one signal. */
export function SourceHealth({ sources }: { sources: Source[] }) {
  if (sources.length === 0) return null;

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {sources.map((s) => {
        const troubled = s.rateLimited + s.failed > 0;
        return (
          <Card key={s.source} className={`p-4 ${troubled ? 'border-error' : ''}`}>
            <Link href={`/sources/${s.source}`} className="font-semibold text-ink hover:underline">
              {s.source}
            </Link>
            <dl className="mt-2 grid grid-cols-4 gap-2 text-center text-xs">
              <Count label="Got" value={s.success} tone="text-success" />
              <Count label="Unavailable" value={s.unavailable} tone="text-warning" />
              <Count label="Rate limited" value={s.rateLimited} tone="text-warning" />
              <Count label="Failed" value={s.failed} tone="text-error" />
            </dl>
          </Card>
        );
      })}
    </div>
  );
}

function Count({ label, value, tone }: { label: string; value: number; tone: string }) {
  return (
    <div>
      <dt className="text-ink-subtle">{label}</dt>
      <dd className={`text-lg font-bold ${value > 0 ? tone : 'text-ink-muted'}`}>{value}</dd>
    </div>
  );
}
