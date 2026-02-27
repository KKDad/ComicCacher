import { render, screen } from '@testing-library/react';
import ComicsPage from './page';

describe('ComicsPage', () => {
  const comics = [
    { id: 1, name: 'Garfield', date: '2024-01-15', thumbnail: 'https://example.com/1.png' },
    { id: 2, name: 'Peanuts', date: '2024-01-15' },
  ];

  it('renders loading skeletons when isLoading', () => {
    render(<ComicsPage isLoading />);
    expect(screen.queryByText('Browse Comics')).not.toBeInTheDocument();
  });

  it('renders empty state when no comics', () => {
    render(<ComicsPage />);
    expect(screen.getByText('No comics available')).toBeInTheDocument();
  });

  it('renders empty state for empty array', () => {
    render(<ComicsPage comics={[]} />);
    expect(screen.getByText('No comics available')).toBeInTheDocument();
  });

  it('renders comic tiles', () => {
    render(<ComicsPage comics={comics} />);
    expect(screen.getByText('Garfield')).toBeInTheDocument();
    expect(screen.getByText('Peanuts')).toBeInTheDocument();
  });

  it('shows comic count', () => {
    render(<ComicsPage comics={comics} />);
    expect(screen.getByText(/2 available comics/)).toBeInTheDocument();
  });

  it('shows Load More button when hasNextPage', () => {
    render(<ComicsPage comics={comics} hasNextPage />);
    expect(screen.getByText('Load More')).toBeInTheDocument();
  });

  it('hides Load More button when no next page', () => {
    render(<ComicsPage comics={comics} />);
    expect(screen.queryByText('Load More')).not.toBeInTheDocument();
  });
});
