import { render, screen } from '@testing-library/react';
import { ContinueReading } from './continue-reading';

describe('ContinueReading', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2024-01-15T12:00:00Z'));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  const lastRead = {
    comic: {
      id: 1,
      name: 'Garfield',
      lastStrip: { imageUrl: 'https://example.com/strip.png' },
    },
    date: '2024-01-15',
  };

  it('renders loading skeleton when isLoading is true', () => {
    render(<ContinueReading isLoading />);
    expect(screen.getByText('Continue Where You Left Off')).toBeInTheDocument();
    expect(screen.queryByText('No recent reading history')).not.toBeInTheDocument();
  });

  it('renders empty state when no lastRead', () => {
    render(<ContinueReading />);
    expect(screen.getByText('No recent reading history')).toBeInTheDocument();
    expect(screen.getByText('Browse Comics')).toBeInTheDocument();
  });

  it('renders comic name when lastRead is provided', () => {
    render(<ContinueReading lastRead={lastRead} />);
    expect(screen.getByText('Garfield')).toBeInTheDocument();
  });

  it('renders strip image when available', () => {
    render(<ContinueReading lastRead={lastRead} />);
    const img = screen.getByAltText('Garfield');
    expect(img).toHaveAttribute('src', 'https://example.com/strip.png');
  });

  it('renders initial fallback when no image', () => {
    const noImage = {
      comic: { id: 1, name: 'Garfield', lastStrip: null },
      date: '2024-01-15',
    };
    render(<ContinueReading lastRead={noImage} />);
    expect(screen.getByText('G')).toBeInTheDocument();
  });

  it('shows "today" for same-day reads', () => {
    render(<ContinueReading lastRead={lastRead} />);
    expect(screen.getByText('Last read: today')).toBeInTheDocument();
  });

  it('shows "1 day ago" for yesterday', () => {
    const yesterday = { ...lastRead, date: '2024-01-14' };
    render(<ContinueReading lastRead={yesterday} />);
    expect(screen.getByText('Last read: 1 day ago')).toBeInTheDocument();
  });

  it('shows "N days ago" for older dates', () => {
    const old = { ...lastRead, date: '2024-01-10' };
    render(<ContinueReading lastRead={old} />);
    expect(screen.getByText('Last read: 5 days ago')).toBeInTheDocument();
  });

  it('links to the correct comic strip page', () => {
    render(<ContinueReading lastRead={lastRead} />);
    const link = screen.getByRole('link', { name: /continue reading/i });
    expect(link).toHaveAttribute('href', '/comics/1/read?date=2024-01-15');
  });
});
