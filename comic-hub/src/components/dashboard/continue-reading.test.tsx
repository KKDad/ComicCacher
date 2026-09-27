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

  it('renders empty state when there is no reading history', () => {
    render(<ContinueReading />);
    expect(screen.getByText('No recent reading history')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Browse comics' })).toHaveAttribute('href', '/comics');
  });

  it('renders comic name when lastRead is provided', () => {
    render(<ContinueReading reads={[lastRead]} />);
    expect(screen.getByRole('heading', { level: 3 })).toHaveTextContent('Garfield');
  });

  it('renders strip image when available', () => {
    render(<ContinueReading reads={[lastRead]} />);
    const img = screen.getByRole('presentation');
    expect(img).toHaveAttribute('src', 'https://example.com/strip.png');
  });

  it('renders initial fallback when no image', () => {
    const noImage = {
      comic: { id: 1, name: 'Garfield', lastStrip: null },
      date: '2024-01-15',
    };
    render(<ContinueReading reads={[noImage]} />);
    expect(screen.getByText('G')).toBeInTheDocument();
  });

  it('shows the strip date the reader has reached', () => {
    render(<ContinueReading reads={[{ ...lastRead, date: '2024-01-10' }]} />);
    expect(screen.getByText('Read up to Jan 10, 2024')).toBeInTheDocument();
  });

  it('lists several comics in one row', () => {
    const reads = [
      lastRead,
      { comic: { id: 2, name: 'Peanuts', lastStrip: null }, date: '2024-01-12' },
      { comic: { id: 3, name: 'Dilbert', lastStrip: null }, date: '2024-01-11', caughtUp: true },
    ];
    render(<ContinueReading reads={reads} />);

    expect(screen.getAllByRole('listitem')).toHaveLength(3);
    expect(screen.getByRole('link', { name: 'Continue reading Peanuts' })).toHaveAttribute('href', '/comics/2/read?date=2024-01-12');
    expect(screen.getByText('All caught up')).toBeInTheDocument();
  });

  it('links to the correct comic strip page', () => {
    render(<ContinueReading reads={[lastRead]} />);
    const link = screen.getByRole('link', { name: /continue reading/i });
    expect(link).toHaveAttribute('href', '/comics/1/read?date=2024-01-15');
  });
});
