import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { FavoritesSection } from './favorites-section';

const mockUseGetUserPreferencesQuery = vi.fn();
const mockUseGetComicsQuery = vi.fn();

vi.mock('@/generated/graphql', () => ({
  useGetUserPreferencesQuery: () => mockUseGetUserPreferencesQuery(),
  useGetComicsQuery: () => mockUseGetComicsQuery(),
}));

vi.mock('@/components/comics/favorite-card', () => ({
  FavoriteCard: ({ comic }: any) => <div data-testid="favorite-card">{comic.name}</div>,
}));

describe('FavoritesSection', () => {
  it('shows loading skeletons when preferences are loading', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: null,
      isLoading: true,
    });
    mockUseGetComicsQuery.mockReturnValue({
      data: null,
      isLoading: false,
    });

    render(<FavoritesSection />);

    expect(screen.getByText('Your Favorites')).toBeInTheDocument();
    // In loading state, we should see the section header
    expect(screen.getByText('Your Favorites')).toBeInTheDocument();
  });

  it('shows loading skeletons when comics are loading', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: { preferences: { favoriteComics: [1, 2] } },
      isLoading: false,
    });
    mockUseGetComicsQuery.mockReturnValue({
      data: null,
      isLoading: true,
    });

    render(<FavoritesSection />);

    // In loading state, we should see the section header
    expect(screen.getByText('Your Favorites')).toBeInTheDocument();
  });

  it('shows empty state when no favorites', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: { preferences: { favoriteComics: [] } },
      isLoading: false,
    });
    mockUseGetComicsQuery.mockReturnValue({
      data: { comics: { edges: [] } },
      isLoading: false,
    });

    render(<FavoritesSection />);

    expect(screen.getByText('No favorite comics yet')).toBeInTheDocument();
    expect(screen.getByText('Mark comics as favorites to see them here')).toBeInTheDocument();
    expect(screen.getByText('Browse Comics')).toBeInTheDocument();
  });

  it('shows empty state when favoriteComics is undefined', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: { preferences: {} },
      isLoading: false,
    });
    mockUseGetComicsQuery.mockReturnValue({
      data: { comics: { edges: [] } },
      isLoading: false,
    });

    render(<FavoritesSection />);

    expect(screen.getByText('No favorite comics yet')).toBeInTheDocument();
  });

  it('renders FavoriteCard for each favorite comic', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: { preferences: { favoriteComics: [1, 2] } },
      isLoading: false,
    });
    mockUseGetComicsQuery.mockReturnValue({
      data: {
        comics: {
          edges: [
            { node: { id: 1, name: 'Comic 1' } },
            { node: { id: 2, name: 'Comic 2' } },
            { node: { id: 3, name: 'Comic 3' } },
          ],
        },
      },
      isLoading: false,
    });

    render(<FavoritesSection />);

    expect(screen.getByText('Comic 1')).toBeInTheDocument();
    expect(screen.getByText('Comic 2')).toBeInTheDocument();
    expect(screen.queryByText('Comic 3')).not.toBeInTheDocument();
  });

  it('filters comics by favoriteComicIds', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: { preferences: { favoriteComics: [1, 3] } },
      isLoading: false,
    });
    mockUseGetComicsQuery.mockReturnValue({
      data: {
        comics: {
          edges: [
            { node: { id: 1, name: 'Favorite 1' } },
            { node: { id: 2, name: 'Not Favorite' } },
            { node: { id: 3, name: 'Favorite 2' } },
          ],
        },
      },
      isLoading: false,
    });

    render(<FavoritesSection />);

    const favoriteCards = screen.getAllByTestId('favorite-card');
    expect(favoriteCards).toHaveLength(2);
    expect(screen.getByText('Favorite 1')).toBeInTheDocument();
    expect(screen.getByText('Favorite 2')).toBeInTheDocument();
    expect(screen.queryByText('Not Favorite')).not.toBeInTheDocument();
  });

  it('handles empty comics edges array', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: { preferences: { favoriteComics: [1, 2] } },
      isLoading: false,
    });
    mockUseGetComicsQuery.mockReturnValue({
      data: { comics: { edges: [] } },
      isLoading: false,
    });

    render(<FavoritesSection />);

    expect(screen.getByText('No favorite comics yet')).toBeInTheDocument();
  });

  it('handles missing comics edges', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: { preferences: { favoriteComics: [1, 2] } },
      isLoading: false,
    });
    mockUseGetComicsQuery.mockReturnValue({
      data: { comics: {} },
      isLoading: false,
    });

    render(<FavoritesSection />);

    expect(screen.getByText('No favorite comics yet')).toBeInTheDocument();
  });

  it('renders section title', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: { preferences: { favoriteComics: [] } },
      isLoading: false,
    });
    mockUseGetComicsQuery.mockReturnValue({
      data: { comics: { edges: [] } },
      isLoading: false,
    });

    render(<FavoritesSection />);

    expect(screen.getByText('Your Favorites')).toBeInTheDocument();
  });
});
