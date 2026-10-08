import { DayOutcome, RetrievalStatusEnum, type GetRetrievalHealthQuery } from '@/generated/graphql';
import { compareNames } from '@/lib/sort';

export type RetrievalHealth = GetRetrievalHealthQuery['retrievalHealth'];
export type ComicHealth = RetrievalHealth['comics'][number];
export type RetrievalDay = ComicHealth['days'][number];
export type RetrievalRecord = NonNullable<RetrievalDay['record']>;
export type TodaysError = RetrievalHealth['todaysErrors'][number];

/** Window lengths the page offers, in days. The query always fetches the longest. */
export const WINDOWS = [7, 14, 30] as const;
export const MAX_WINDOW = WINDOWS[WINDOWS.length - 1];

/** An active comic missing its latest expected strip, or one whose newest strip is older than its schedule allows. */
export function needsAttention(comic: ComicHealth): boolean {
  return comic.active && (comic.missingStreak > 0 || comic.stale);
}

/** Worst first: the longest missing streak, then stale, then by name. */
export function bySeverity(a: ComicHealth, b: ComicHealth): number {
  return (
    b.missingStreak - a.missingStreak ||
    Number(b.stale) - Number(a.stale) ||
    compareNames(a.comicName, b.comicName)
  );
}

/** "RATE LIMITED" from RATE_LIMITED. */
export function statusLabel(status: RetrievalStatusEnum): string {
  return status.replace(/_/g, ' ');
}

const outcomeLabels: Record<DayOutcome, string> = {
  [DayOutcome.OnDisk]: 'on disk',
  [DayOutcome.Missing]: 'missing',
  [DayOutcome.OffDay]: 'no strip expected',
  [DayOutcome.Pending]: 'waiting for today’s run',
};

/** "missing: RATE LIMITED (HTTP 429)", or "on disk, recovered after NETWORK ERROR". */
export function describeDay(day: RetrievalDay): string {
  const label = outcomeLabels[day.outcome];
  const record = day.record;
  if (day.outcome === DayOutcome.OnDisk) {
    return day.recovered && record ? `${label}, recovered after ${statusLabel(record.status)}` : label;
  }
  if (day.outcome === DayOutcome.Missing) {
    if (!record) return `${label}: not attempted`;
    const http = record.httpStatusCode != null ? ` (HTTP ${record.httpStatusCode})` : '';
    return `${label}: ${statusLabel(record.status)}${http}`;
  }
  return label;
}

/** The comics' results on one day: how many are on disk, missing and still pending. */
export function countDay(comics: ComicHealth[], date: string) {
  const counts = { onDisk: 0, missing: 0, pending: 0 };
  for (const comic of comics) {
    const day = comic.days.find((d) => d.date === date);
    if (day?.outcome === DayOutcome.OnDisk) counts.onDisk++;
    else if (day?.outcome === DayOutcome.Missing) counts.missing++;
    else if (day?.outcome === DayOutcome.Pending) counts.pending++;
  }
  return counts;
}

export function isFailure(status: RetrievalStatusEnum): boolean {
  return status !== RetrievalStatusEnum.Success && status !== RetrievalStatusEnum.ComicUnavailable;
}
