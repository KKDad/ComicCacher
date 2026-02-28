import { render, screen } from '@testing-library/react';
import { TodaysComics } from './todays-comics';

describe('TodaysComics', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2024-01-15T12:00:00Z'));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  const comics = [
    { id: 1, name: 'Garfield', date: '2024-01-15', thumbnail: 'https://example.com/1.png' },
    { id: 2, name: 'Peanuts', date: '2024-01-15', thumbnail: 'https://example.com/2.png' },
  ];

  it('renders loading skeletons when isLoading', () => {
    render(<TodaysComics isLoading />);
    expect(screen.getByText("Today's Comics")).toBeInTheDocument();
    expect(screen.queryByText('No comics for today')).not.toBeInTheDocument();
  });

  it('renders empty state when no comics', () => {
    render(<TodaysComics />);
    expect(screen.getByText('No comics for today')).toBeInTheDocument();
    expect(screen.getByText('View Archive')).toBeInTheDocument();
  });

  it('renders empty state for empty array', () => {
    render(<TodaysComics comics={[]} />);
    expect(screen.getByText('No comics for today')).toBeInTheDocument();
  });

  it('renders comic tiles when comics provided', () => {
    render(<TodaysComics comics={comics} />);
    expect(screen.getByText('Garfield')).toBeInTheDocument();
    expect(screen.getByText('Peanuts')).toBeInTheDocument();
  });

  it('shows View All button as link to /comics', () => {
    render(<TodaysComics comics={comics} />);
    const viewAllLink = screen.getByRole('link', { name: /view all/i });
    expect(viewAllLink).toHaveAttribute('href', '/comics');
  });

  it('displays formatted date', () => {
    render(<TodaysComics comics={comics} />);
    expect(screen.getByText('Monday, January 15, 2024')).toBeInTheDocument();
  });
});
