import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ContinueReading } from './continue-reading';

const mockUseGetUserPreferencesQuery = vi.fn();
const mockUseGetComicQuery = vi.fn();

vi.mock('@/generated/graphql', () => ({
  useGetUserPreferencesQuery: () => mockUseGetUserPreferencesQuery(),
  useGetComicQuery: (...args: any[]) => mockUseGetComicQuery(...args),
}));

describe('ContinueReading', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2024-01-15T10:00:00'));
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('shows loading skeleton when preferences are loading', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: null,
      isLoading: true,
    });
    mockUseGetComicQuery.mockReturnValue({
      data: null,
      isLoading: false,
    });

    render(<ContinueReading />);

    expect(screen.getByText('Continue Where You Left Off')).toBeInTheDocument();
    // In loading state, we should see the section header
    expect(screen.getByText('Continue Where You Left Off')).toBeInTheDocument();
  });

  it('shows loading skeleton when comic is loading', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: {
        preferences: {
          lastReadDates: [{ comicId: 1, date: '2024-01-14' }],
        },
      },
      isLoading: false,
    });
    mockUseGetComicQuery.mockReturnValue({
      data: null,
      isLoading: true,
    });

    render(<ContinueReading />);

    // In loading state, we should see the section header
    expect(screen.getByText('Continue Where You Left Off')).toBeInTheDocument();
  });

  it('shows empty state when no reading history', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: { preferences: { lastReadDates: [] } },
      isLoading: false,
    });
    mockUseGetComicQuery.mockReturnValue({
      data: null,
      isLoading: false,
    });

    render(<ContinueReading />);

    expect(screen.getByText('No recent reading history')).toBeInTheDocument();
    expect(screen.getByText('Start reading to see your progress here')).toBeInTheDocument();
    expect(screen.getByText('Browse Comics')).toBeInTheDocument();
  });

  it('shows empty state when lastReadDates is undefined', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: { preferences: {} },
      isLoading: false,
    });
    mockUseGetComicQuery.mockReturnValue({
      data: null,
      isLoading: false,
    });

    render(<ContinueReading />);

    expect(screen.getByText('No recent reading history')).toBeInTheDocument();
  });

  it('shows last read comic with Continue button', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: {
        preferences: {
          lastReadDates: [{ comicId: 1, date: '2024-01-14' }],
        },
      },
      isLoading: false,
    });
    mockUseGetComicQuery.mockReturnValue({
      data: {
        comic: {
          id: 1,
          name: 'Test Comic',
          lastStrip: { imageUrl: 'image.jpg' },
        },
      },
      isLoading: false,
    });

    render(<ContinueReading />);

    expect(screen.getByText('Test Comic')).toBeInTheDocument();
    expect(screen.getByText('Continue Reading')).toBeInTheDocument();
  });

  it('displays "today" when last read today', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: {
        preferences: {
          lastReadDates: [{ comicId: 1, date: '2024-01-15' }],
        },
      },
      isLoading: false,
    });
    mockUseGetComicQuery.mockReturnValue({
      data: {
        comic: {
          id: 1,
          name: 'Test Comic',
          lastStrip: { imageUrl: 'image.jpg' },
        },
      },
      isLoading: false,
    });

    render(<ContinueReading />);

    expect(screen.getByText('Last read: today')).toBeInTheDocument();
  });

  it('displays "1 day ago" when last read yesterday', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: {
        preferences: {
          lastReadDates: [{ comicId: 1, date: '2024-01-14' }],
        },
      },
      isLoading: false,
    });
    mockUseGetComicQuery.mockReturnValue({
      data: {
        comic: {
          id: 1,
          name: 'Test Comic',
          lastStrip: { imageUrl: 'image.jpg' },
        },
      },
      isLoading: false,
    });

    render(<ContinueReading />);

    expect(screen.getByText('Last read: 1 day ago')).toBeInTheDocument();
  });

  it('displays correct days ago for multiple days', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: {
        preferences: {
          lastReadDates: [{ comicId: 1, date: '2024-01-10' }],
        },
      },
      isLoading: false,
    });
    mockUseGetComicQuery.mockReturnValue({
      data: {
        comic: {
          id: 1,
          name: 'Test Comic',
          lastStrip: { imageUrl: 'image.jpg' },
        },
      },
      isLoading: false,
    });

    render(<ContinueReading />);

    expect(screen.getByText('Last read: 5 days ago')).toBeInTheDocument();
  });

  it('sorts lastReadDates and shows most recent', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: {
        preferences: {
          lastReadDates: [
            { comicId: 1, date: '2024-01-10' },
            { comicId: 2, date: '2024-01-14' },
            { comicId: 3, date: '2024-01-12' },
          ],
        },
      },
      isLoading: false,
    });
    mockUseGetComicQuery.mockReturnValue({
      data: {
        comic: {
          id: 2,
          name: 'Most Recent Comic',
          lastStrip: { imageUrl: 'image.jpg' },
        },
      },
      isLoading: false,
    });

    render(<ContinueReading />);

    expect(screen.getByText('Most Recent Comic')).toBeInTheDocument();
    expect(screen.getByText('Last read: 1 day ago')).toBeInTheDocument();
  });

  it('renders thumbnail image when available', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: {
        preferences: {
          lastReadDates: [{ comicId: 1, date: '2024-01-14' }],
        },
      },
      isLoading: false,
    });
    mockUseGetComicQuery.mockReturnValue({
      data: {
        comic: {
          id: 1,
          name: 'Test Comic',
          lastStrip: { imageUrl: 'https://example.com/image.jpg' },
        },
      },
      isLoading: false,
    });

    render(<ContinueReading />);

    const image = screen.getByRole('img', { name: 'Test Comic' });
    expect(image).toBeInTheDocument();
    expect(image).toHaveAttribute('src', 'https://example.com/image.jpg');
  });

  it('renders fallback initial when no thumbnail', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: {
        preferences: {
          lastReadDates: [{ comicId: 1, date: '2024-01-14' }],
        },
      },
      isLoading: false,
    });
    mockUseGetComicQuery.mockReturnValue({
      data: {
        comic: {
          id: 1,
          name: 'Test Comic',
          lastStrip: null,
        },
      },
      isLoading: false,
    });

    render(<ContinueReading />);

    expect(screen.queryByRole('img')).not.toBeInTheDocument();
    expect(screen.getByText('T')).toBeInTheDocument();
  });

  it('link navigates to correct comic URL', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: {
        preferences: {
          lastReadDates: [{ comicId: 1, date: '2024-01-14' }],
        },
      },
      isLoading: false,
    });
    mockUseGetComicQuery.mockReturnValue({
      data: {
        comic: {
          id: 1,
          name: 'Test Comic',
          lastStrip: { imageUrl: 'image.jpg' },
        },
      },
      isLoading: false,
    });

    render(<ContinueReading />);

    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('href', '/comics/1/2024-01-14');
  });

  it('passes enabled option to useGetComicQuery', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: { preferences: { lastReadDates: [] } },
      isLoading: false,
    });
    mockUseGetComicQuery.mockReturnValue({
      data: null,
      isLoading: false,
    });

    render(<ContinueReading />);

    expect(mockUseGetComicQuery).toHaveBeenCalledWith(
      { id: 0 },
      expect.objectContaining({ enabled: false })
    );
  });

  it('renders section title', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: { preferences: { lastReadDates: [] } },
      isLoading: false,
    });
    mockUseGetComicQuery.mockReturnValue({
      data: null,
      isLoading: false,
    });

    render(<ContinueReading />);

    expect(screen.getByText('Continue Where You Left Off')).toBeInTheDocument();
  });
});
