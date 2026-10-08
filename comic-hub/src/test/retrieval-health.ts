import { DayOutcome, RetrievalStatusEnum } from '@/generated/graphql';
import type { ComicHealth, RetrievalDay, RetrievalHealth, RetrievalRecord } from '@/components/retrieval-status/health';
import { shiftIsoDate } from '@/lib/date-utils';

export const TARGET_DATE = '2026-10-08';

export function record(comicDate: string, status: RetrievalStatusEnum, extra: Partial<RetrievalRecord> = {}): RetrievalRecord {
  return {
    id: `1_${comicDate}`,
    comicDate,
    status,
    errorMessage: status === RetrievalStatusEnum.Success ? null : `${status} message`,
    httpStatusCode: null,
    retrievalDurationMs: 120,
    imageSize: status === RetrievalStatusEnum.Success ? 2048 : null,
    attemptedAt: `${comicDate}T11:00:00Z`,
    ...extra,
  };
}

/** Days ending on TARGET_DATE, oldest first; `outcomes` gives the newest days, the rest are ON_DISK. */
export function days(count: number, outcomes: Record<string, Partial<RetrievalDay>> = {}): RetrievalDay[] {
  return Array.from({ length: count }, (_, i) => {
    const date = shiftIsoDate(TARGET_DATE, i - count + 1);
    return { date, outcome: DayOutcome.OnDisk, recovered: false, record: null, ...outcomes[date] };
  });
}

export function comic(comicId: number, comicName: string, extra: Partial<ComicHealth> = {}): ComicHealth {
  return {
    comicId,
    comicName,
    source: 'gocomics',
    enabled: true,
    active: true,
    indexed: false,
    publicationDays: null,
    newest: TARGET_DATE,
    expectedLatest: TARGET_DATE,
    stale: false,
    missingStreak: 0,
    latestError: null,
    days: days(30),
    ...extra,
  };
}

export function health(extra: Partial<RetrievalHealth> = {}): RetrievalHealth {
  return {
    targetDate: TARGET_DATE,
    lastRun: {
      executionId: 42,
      jobName: 'ComicDownloadJob',
      status: 'COMPLETED',
      startTime: '2026-10-08T11:00:00Z',
      endTime: '2026-10-08T11:05:00Z',
      durationMs: 300000,
    },
    sources: [{ source: 'gocomics', success: 2, unavailable: 0, rateLimited: 1, failed: 0 }],
    todaysErrors: [],
    comics: [],
    ...extra,
  };
}
