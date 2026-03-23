import { describe, it, expect, vi, afterEach } from 'vitest';
import {
  formatShortDate,
  formatFullDate,
  formatAbsoluteTime,
  formatRelativeTime,
  formatDuration,
} from './date-utils';

describe('date-utils', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  describe('formatShortDate', () => {
    it('formats as abbreviated month and day', () => {
      expect(formatShortDate('2026-03-18T12:00:00Z')).toBe('Mar 18');
    });

    it('handles single-digit days', () => {
      expect(formatShortDate('2026-01-05T12:00:00Z')).toBe('Jan 5');
    });
  });

  describe('formatFullDate', () => {
    it('includes weekday, full month, day, and year', () => {
      const result = formatFullDate('2026-03-18T12:00:00Z');
      expect(result).toContain('2026');
      expect(result).toContain('March');
      expect(result).toContain('18');
    });
  });

  describe('formatAbsoluteTime', () => {
    it('includes month, day, and time', () => {
      const result = formatAbsoluteTime('2026-03-18T15:30:00Z');
      expect(result).toContain('Mar');
      expect(result).toContain('18');
    });
  });

  describe('formatRelativeTime', () => {
    it('returns "just now" for times within a minute', () => {
      const now = new Date();
      expect(formatRelativeTime(now.toISOString())).toBe('just now');
    });

    it('returns minutes ago for recent past', () => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date('2026-03-18T12:10:00Z'));
      expect(formatRelativeTime('2026-03-18T12:05:00Z')).toBe('5m ago');
      vi.useRealTimers();
    });

    it('returns hours and minutes ago', () => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date('2026-03-18T14:15:00Z'));
      expect(formatRelativeTime('2026-03-18T12:00:00Z')).toBe('2h 15m ago');
      vi.useRealTimers();
    });

    it('returns days ago for past dates', () => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date('2026-03-18T12:00:00Z'));
      expect(formatRelativeTime('2026-03-15T12:00:00Z')).toBe('3 days ago');
      vi.useRealTimers();
    });

    it('returns "1 day ago" for yesterday', () => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date('2026-03-18T12:00:00Z'));
      expect(formatRelativeTime('2026-03-17T12:00:00Z')).toBe('1 day ago');
      vi.useRealTimers();
    });

    it('returns future times with "in" prefix', () => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date('2026-03-18T12:00:00Z'));
      expect(formatRelativeTime('2026-03-18T12:30:00Z')).toBe('in 30m');
      vi.useRealTimers();
    });

    it('returns future hours', () => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date('2026-03-18T12:00:00Z'));
      expect(formatRelativeTime('2026-03-18T14:15:00Z')).toBe('in 2h 15m');
      vi.useRealTimers();
    });
  });

  describe('formatDuration', () => {
    it('formats milliseconds', () => {
      expect(formatDuration(450)).toBe('450ms');
    });

    it('formats seconds', () => {
      expect(formatDuration(1200)).toBe('1.2s');
    });

    it('formats minutes and seconds', () => {
      expect(formatDuration(192_000)).toBe('3m 12s');
    });
  });
});
