import { render, screen } from '@testing-library/react';
import { FavoritesSection } from './favorites-section';

describe('FavoritesSection', () => {
  const favorites = [
    { id: 1, name: 'Garfield', avatarUrl: null },
    { id: 2, name: 'Calvin and Hobbes', avatarUrl: null },
  ];

  it('renders loading skeletons when isLoading', () => {
    render(<FavoritesSection isLoading />);
    expect(screen.getByText('Your Favorites')).toBeInTheDocument();
    expect(screen.queryByText('No favorite comics yet')).not.toBeInTheDocument();
  });

  it('renders empty state when no favorites', () => {
    render(<FavoritesSection />);
    expect(screen.getByText('No favorite comics yet')).toBeInTheDocument();
    const browseLink = screen.getByRole('link', { name: /browse comics/i });
    expect(browseLink).toHaveAttribute('href', '/comics');
  });

  it('renders empty state for empty array', () => {
    render(<FavoritesSection favorites={[]} />);
    expect(screen.getByText('No favorite comics yet')).toBeInTheDocument();
  });

  it('renders favorite cards when favorites provided', () => {
    render(<FavoritesSection favorites={favorites} />);
    expect(screen.getByText('Garfield')).toBeInTheDocument();
    expect(screen.getByText('Calvin and Hobbes')).toBeInTheDocument();
  });

  it('renders correct number of favorite cards', () => {
    render(<FavoritesSection favorites={favorites} />);
    const links = screen.getAllByRole('link');
    expect(links).toHaveLength(2);
  });
});
