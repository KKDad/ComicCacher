import { Check, X } from 'lucide-react';

/** The grid's key: what each kind of cell means. */
export function ResultsLegend() {
  return (
    <ul aria-label="Key" className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-ink-subtle">
      <li className="flex items-center gap-1.5">
        <span className="flex h-5 w-5 items-center justify-center text-success">
          <Check className="h-3.5 w-3.5" />
        </span>
        On disk
      </li>
      <li className="flex items-center gap-1.5">
        <span className="flex h-5 w-5 items-center justify-center rounded text-success ring-1 ring-warning ring-inset">
          <Check className="h-3.5 w-3.5" />
        </span>
        Recovered after a failure
      </li>
      <li className="flex items-center gap-1.5">
        <span className="flex h-5 w-5 items-center justify-center rounded bg-error-subtle text-error">
          <X className="h-3.5 w-3.5" />
        </span>
        Missing
      </li>
      <li className="flex items-center gap-1.5">
        <span className="h-5 w-5 rounded border border-dashed border-border" />
        No strip due
      </li>
      <li className="flex items-center gap-1.5">
        <span className="flex h-5 w-5 items-center justify-center text-ink-muted">·</span>
        Waiting for today’s run
      </li>
      <li className="text-ink-muted">Click a comic for its attempts; ↑ and ↓ in the grid move between comics</li>
    </ul>
  );
}
