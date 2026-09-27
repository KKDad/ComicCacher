/**
 * Shared date formatting utilities.
 *
 * All functions accept an ISO-8601 string and return a locale-aware display string.
 * Never assume a timezone — let the browser handle it via toLocaleString().
 */

const DATE_ONLY = /^(\d{4})-(\d{2})-(\d{2})$/;

/**
 * Parses an ISO string, reading a bare `YYYY-MM-DD` (a strip date) as that
 * calendar day in local time. `new Date('2026-03-18')` is UTC midnight, which
 * renders as March 17 anywhere west of Greenwich.
 */
export function parseDate(dateStr: string): Date {
  const m = DATE_ONLY.exec(dateStr);
  if (m) return new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]));
  return new Date(dateStr);
}

/** A date's local calendar day as `YYYY-MM-DD`, the strip-date format. */
export function toIsoDate(date: Date): string {
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, '0');
  const dd = String(date.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}

/** Today's local date as `YYYY-MM-DD`. */
export function todayIsoDate(): string {
  return toIsoDate(new Date());
}

/** The `YYYY-MM-DD` date `days` calendar days after (or before, if negative) `dateStr`. */
export function shiftIsoDate(dateStr: string, days: number): string {
  const d = parseDate(dateStr);
  d.setDate(d.getDate() + days);
  return toIsoDate(d);
}

/** "Mar 18" — compact date for cards and lists. */
export function formatShortDate(dateStr: string): string {
  return parseDate(dateStr).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
  });
}

/** "Wed, March 18, 2026" — full date for reader strips. */
export function formatFullDate(dateStr: string): string {
  return parseDate(dateStr).toLocaleDateString('en-US', {
    weekday: 'short',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
}

/** "Mar 18, 2026" — medium date for captions. */
export function formatMediumDate(dateStr: string): string {
  return parseDate(dateStr).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

/** "Mar 18, 3:00 PM" — date + time for execution logs and scheduling. */
export function formatAbsoluteTime(dateStr: string): string {
  return new Date(dateStr).toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
}

/** "in 2h 15m", "3m ago", "just now", "today", "1 day ago", "5 days ago". */
export function formatRelativeTime(dateStr: string): string {
  const date = new Date(dateStr);
  const now = new Date();
  const diffMs = date.getTime() - now.getTime();
  const absDiffMs = Math.abs(diffMs);

  if (absDiffMs < 60_000) return 'just now';

  const minutes = Math.floor(absDiffMs / 60_000);
  const hours = Math.floor(absDiffMs / 3_600_000);
  const days = Math.floor(absDiffMs / 86_400_000);

  if (diffMs > 0) {
    if (hours > 0) return `in ${hours}h ${minutes % 60}m`;
    return `in ${minutes}m`;
  }

  if (days >= 1) {
    if (days === 1) return '1 day ago';
    return `${days} days ago`;
  }
  if (hours > 0) return `${hours}h ${minutes % 60}m ago`;
  return `${minutes}m ago`;
}

/** "just now", "12m ago", "3h ago", "5d ago" — compact past time for summaries and tables. */
export function formatTimeAgo(dateStr: string): string {
  const seconds = Math.floor((Date.now() - new Date(dateStr).getTime()) / 1000);
  if (seconds < 60) return 'just now';
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  return `${Math.floor(hours / 24)}d ago`;
}

/** "1.2s", "3m 12s", "450ms" — elapsed duration for job executions. */
export function formatDuration(ms: number): string {
  if (ms < 1000) return `${Math.round(ms)}ms`;
  const seconds = ms / 1000;
  if (seconds < 60) return `${seconds.toFixed(1)}s`;
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = Math.round(seconds % 60);
  return `${minutes}m ${remainingSeconds}s`;
}
