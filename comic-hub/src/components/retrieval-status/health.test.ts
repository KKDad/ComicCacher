import { DayOutcome, RetrievalStatusEnum } from '@/generated/graphql';
import { comic, days, record, TARGET_DATE } from '@/test/retrieval-health';
import { bySeverity, countDay, describeDay, isFailure, needsAttention } from './health';

describe('needsAttention', () => {
  it('flags an active comic with a missing streak or a stale newest strip', () => {
    expect(needsAttention(comic(1, 'A', { missingStreak: 1 }))).toBe(true);
    expect(needsAttention(comic(1, 'A', { stale: true }))).toBe(true);
    expect(needsAttention(comic(1, 'A'))).toBe(false);
  });

  it('leaves inactive comics alone', () => {
    expect(needsAttention(comic(1, 'A', { active: false, missingStreak: 3, stale: true }))).toBe(false);
  });
});

describe('bySeverity', () => {
  it('puts the longest streak first, then stale, then by name', () => {
    const sorted = [
      comic(1, 'beta'),
      comic(2, 'Alpha'),
      comic(3, 'Stale', { stale: true }),
      comic(4, 'Two', { missingStreak: 2 }),
      comic(5, 'One', { missingStreak: 1 }),
    ].toSorted(bySeverity);

    expect(sorted.map((c) => c.comicName)).toEqual(['Two', 'One', 'Stale', 'Alpha', 'beta']);
  });
});

describe('describeDay', () => {
  it('names the failure and HTTP status of a missing day', () => {
    const day = {
      date: TARGET_DATE,
      outcome: DayOutcome.Missing,
      recovered: false,
      record: record(TARGET_DATE, RetrievalStatusEnum.RateLimited, { httpStatusCode: 429 }),
    };
    expect(describeDay(day)).toBe('missing: RATE LIMITED (HTTP 429)');
  });

  it('says a missing day without a record was not attempted', () => {
    expect(describeDay({ date: TARGET_DATE, outcome: DayOutcome.Missing, recovered: false, record: null })).toBe('missing: not attempted');
  });

  it('says what a recovered strip got over', () => {
    const day = { date: TARGET_DATE, outcome: DayOutcome.OnDisk, recovered: true, record: record(TARGET_DATE, RetrievalStatusEnum.NetworkError) };
    expect(describeDay(day)).toBe('on disk, recovered after NETWORK ERROR');
  });

  it.each([
    [DayOutcome.OnDisk, 'on disk'],
    [DayOutcome.OffDay, 'no strip expected'],
    [DayOutcome.Pending, 'waiting for today’s run'],
  ])('labels %s', (outcome, label) => {
    expect(describeDay({ date: TARGET_DATE, outcome, recovered: false, record: null })).toBe(label);
  });
});

describe('countDay', () => {
  it('counts the comics on disk, missing and pending on a day', () => {
    const comics = [
      comic(1, 'A'),
      comic(2, 'B', { days: days(3, { [TARGET_DATE]: { outcome: DayOutcome.Missing } }) }),
      comic(3, 'C', { days: days(3, { [TARGET_DATE]: { outcome: DayOutcome.Pending } }) }),
      comic(4, 'D', { days: days(3, { [TARGET_DATE]: { outcome: DayOutcome.OffDay } }) }),
    ];
    expect(countDay(comics, TARGET_DATE)).toEqual({ onDisk: 1, missing: 1, pending: 1 });
  });
});

describe('isFailure', () => {
  it('counts neither success nor an unavailable strip', () => {
    expect(isFailure(RetrievalStatusEnum.Success)).toBe(false);
    expect(isFailure(RetrievalStatusEnum.ComicUnavailable)).toBe(false);
    expect(isFailure(RetrievalStatusEnum.RateLimited)).toBe(true);
  });
});
