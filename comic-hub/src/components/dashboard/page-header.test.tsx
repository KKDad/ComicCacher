import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { PageHeader } from './page-header';

describe('PageHeader', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('displays provided displayName', () => {
    vi.setSystemTime(new Date('2024-01-15T10:00:00'));
    render(<PageHeader displayName="John" />);
    expect(screen.getByText(/John/)).toBeInTheDocument();
  });

  describe('time-based greetings', () => {
    it('shows "Good morning" at 5 AM', () => {
      vi.setSystemTime(new Date('2024-01-15T05:00:00'));
      render(<PageHeader displayName="Alice" />);
      expect(screen.getByText(/Good morning, Alice!/)).toBeInTheDocument();
    });

    it('shows "Good morning" at 11 AM', () => {
      vi.setSystemTime(new Date('2024-01-15T11:00:00'));
      render(<PageHeader displayName="Bob" />);
      expect(screen.getByText(/Good morning, Bob!/)).toBeInTheDocument();
    });

    it('shows "Good morning" at 11:59 AM', () => {
      vi.setSystemTime(new Date('2024-01-15T11:59:00'));
      render(<PageHeader displayName="Charlie" />);
      expect(screen.getByText(/Good morning, Charlie!/)).toBeInTheDocument();
    });

    it('shows "Good afternoon" at 12 PM', () => {
      vi.setSystemTime(new Date('2024-01-15T12:00:00'));
      render(<PageHeader displayName="David" />);
      expect(screen.getByText(/Good afternoon, David!/)).toBeInTheDocument();
    });

    it('shows "Good afternoon" at 3 PM', () => {
      vi.setSystemTime(new Date('2024-01-15T15:00:00'));
      render(<PageHeader displayName="Eve" />);
      expect(screen.getByText(/Good afternoon, Eve!/)).toBeInTheDocument();
    });

    it('shows "Good afternoon" at 5:59 PM', () => {
      vi.setSystemTime(new Date('2024-01-15T17:59:00'));
      render(<PageHeader displayName="Frank" />);
      expect(screen.getByText(/Good afternoon, Frank!/)).toBeInTheDocument();
    });

    it('shows "Good evening" at 6 PM', () => {
      vi.setSystemTime(new Date('2024-01-15T18:00:00'));
      render(<PageHeader displayName="Grace" />);
      expect(screen.getByText(/Good evening, Grace!/)).toBeInTheDocument();
    });

    it('shows "Good evening" at 11 PM', () => {
      vi.setSystemTime(new Date('2024-01-15T23:00:00'));
      render(<PageHeader displayName="Henry" />);
      expect(screen.getByText(/Good evening, Henry!/)).toBeInTheDocument();
    });

    it('shows "Good morning" at midnight', () => {
      vi.setSystemTime(new Date('2024-01-15T00:00:00'));
      render(<PageHeader displayName="Iris" />);
      expect(screen.getByText(/Good morning, Iris!/)).toBeInTheDocument();
    });

    it('shows "Good morning" at 4 AM', () => {
      vi.setSystemTime(new Date('2024-01-15T04:00:00'));
      render(<PageHeader displayName="Jack" />);
      expect(screen.getByText(/Good morning, Jack!/)).toBeInTheDocument();
    });
  });

  it('displays subtitle text', () => {
    vi.setSystemTime(new Date('2024-01-15T10:00:00'));
    render(<PageHeader displayName="User" />);
    expect(screen.getByText("Here's what's happening with your comics today")).toBeInTheDocument();
  });
});
