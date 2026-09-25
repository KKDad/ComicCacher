import { render, screen } from '@testing-library/react';
import { LatestUpdates } from './latest-updates';

describe('LatestUpdates', () => {
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
    render(<LatestUpdates isLoading />);
    expect(screen.getByText('Latest Updates')).toBeInTheDocument();
    expect(screen.queryByText('No comics for today')).not.toBeInTheDocument();
  });

  it('renders empty state when no comics', () => {
    render(<LatestUpdates />);
    expect(screen.getByText('No new strips yet')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Browse comics' })).toHaveAttribute('href', '/comics');
  });

  it('renders empty state for empty array', () => {
    render(<LatestUpdates comics={[]} />);
    expect(screen.getByText('No new strips yet')).toBeInTheDocument();
  });

  it('renders comic tiles when comics provided', () => {
    render(<LatestUpdates comics={comics} />);
    expect(screen.getByText('Garfield')).toBeInTheDocument();
    expect(screen.getByText('Peanuts')).toBeInTheDocument();
  });

  it('shows View All button as link to /comics', () => {
    render(<LatestUpdates comics={comics} />);
    const viewAllLink = screen.getByRole('link', { name: /view all/i });
    expect(viewAllLink).toHaveAttribute('href', '/comics');
  });

  it('shows each strip date on its tile', () => {
    render(<LatestUpdates comics={comics} />);
    expect(screen.getAllByText('Jan 15')).toHaveLength(2);
  });

  it('renders tiles as a list', () => {
    render(<LatestUpdates comics={comics} />);
    expect(screen.getAllByRole('listitem')).toHaveLength(2);
  });
});
