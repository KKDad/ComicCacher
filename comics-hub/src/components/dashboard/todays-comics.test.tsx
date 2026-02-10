import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { TodaysComics } from './todays-comics';

const mockUseGetUserPreferencesQuery = vi.fn();
const mockUseGetComicsQuery = vi.fn();

vi.mock('@/generated/graphql', () => ({
  useGetUserPreferencesQuery: () => mockUseGetUserPreferencesQuery(),
  useGetComicsQuery: () => mockUseGetComicsQuery(),
}));

vi.mock('@/components/comics/comic-tile', () => ({
  ComicTile: ({ comic }: any) => (
    <div data-testid="comic-tile">
      {comic.name} - {comic.date}
    </div>
  ),
}));

describe('TodaysComics', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2024-01-15T10:00:00'));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('shows loading skeleton grid when loading', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: null,
    });
    mockUseGetComicsQuery.mockReturnValue({
      data: null,
      isLoading: true,
    });

    const { container } = render(<TodaysComics />);

    expect(screen.getByText("Today's Comics")).toBeInTheDocument();
    // Check for Card components (8 skeleton cards in grid)
    const cards = container.querySelectorAll('[data-slot="card"]');
    expect(cards.length).toBe(8);
  });

  it('shows empty state when no comics available', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: { preferences: { favoriteComics: [] } },
    });
    mockUseGetComicsQuery.mockReturnValue({
      data: { comics: { edges: [] } },
      isLoading: false,
    });

    render(<TodaysComics />);

    expect(screen.getByText('No comics for today')).toBeInTheDocument();
    expect(screen.getByText('Check back later or browse the archive')).toBeInTheDocument();
    expect(screen.getByText('View Archive')).toBeInTheDocument();
  });

  it('shows empty state when comics have no lastStrip', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: { preferences: { favoriteComics: [] } },
    });
    mockUseGetComicsQuery.mockReturnValue({
      data: {
        comics: {
          edges: [
            { node: { id: 1, name: 'Comic 1', lastStrip: null } },
            { node: { id: 2, name: 'Comic 2', lastStrip: { imageUrl: null } } },
          ],
        },
      },
      isLoading: false,
    });

    render(<TodaysComics />);

    expect(screen.getByText('No comics for today')).toBeInTheDocument();
  });

  it('renders ComicTile for each comic with lastStrip', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: { preferences: { favoriteComics: [] } },
    });
    mockUseGetComicsQuery.mockReturnValue({
      data: {
        comics: {
          edges: [
            {
              node: {
                id: 1,
                name: 'Comic 1',
                lastStrip: { imageUrl: 'image1.jpg', date: '2024-01-15' },
              },
            },
            {
              node: {
                id: 2,
                name: 'Comic 2',
                lastStrip: { imageUrl: 'image2.jpg', date: '2024-01-15' },
              },
            },
          ],
        },
      },
      isLoading: false,
    });

    render(<TodaysComics />);

    expect(screen.getByText('Comic 1 - 2024-01-15')).toBeInTheDocument();
    expect(screen.getByText('Comic 2 - 2024-01-15')).toBeInTheDocument();
  });

  it('filters comics by user favorites when favorites exist', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: { preferences: { favoriteComics: [1, 3] } },
    });
    mockUseGetComicsQuery.mockReturnValue({
      data: {
        comics: {
          edges: [
            {
              node: {
                id: 1,
                name: 'Favorite 1',
                lastStrip: { imageUrl: 'image1.jpg', date: '2024-01-15' },
              },
            },
            {
              node: {
                id: 2,
                name: 'Not Favorite',
                lastStrip: { imageUrl: 'image2.jpg', date: '2024-01-15' },
              },
            },
            {
              node: {
                id: 3,
                name: 'Favorite 2',
                lastStrip: { imageUrl: 'image3.jpg', date: '2024-01-15' },
              },
            },
          ],
        },
      },
      isLoading: false,
    });

    render(<TodaysComics />);

    const comicTiles = screen.getAllByTestId('comic-tile');
    expect(comicTiles).toHaveLength(2);
    expect(screen.getByText('Favorite 1 - 2024-01-15')).toBeInTheDocument();
    expect(screen.getByText('Favorite 2 - 2024-01-15')).toBeInTheDocument();
    expect(screen.queryByText('Not Favorite - 2024-01-15')).not.toBeInTheDocument();
  });

  it('shows all comics when no favorites are set', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: { preferences: { favoriteComics: [] } },
    });
    mockUseGetComicsQuery.mockReturnValue({
      data: {
        comics: {
          edges: [
            {
              node: {
                id: 1,
                name: 'Comic 1',
                lastStrip: { imageUrl: 'image1.jpg', date: '2024-01-15' },
              },
            },
            {
              node: {
                id: 2,
                name: 'Comic 2',
                lastStrip: { imageUrl: 'image2.jpg', date: '2024-01-15' },
              },
            },
          ],
        },
      },
      isLoading: false,
    });

    render(<TodaysComics />);

    const comicTiles = screen.getAllByTestId('comic-tile');
    expect(comicTiles).toHaveLength(2);
  });

  it('displays formatted date', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: { preferences: { favoriteComics: [] } },
    });
    mockUseGetComicsQuery.mockReturnValue({
      data: { comics: { edges: [] } },
      isLoading: false,
    });

    render(<TodaysComics />);

    expect(screen.getByText('Monday, January 15, 2024')).toBeInTheDocument();
  });

  it('renders section title', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: { preferences: { favoriteComics: [] } },
    });
    mockUseGetComicsQuery.mockReturnValue({
      data: { comics: { edges: [] } },
      isLoading: false,
    });

    render(<TodaysComics />);

    expect(screen.getByText("Today's Comics")).toBeInTheDocument();
  });

  it('renders View All button when comics exist', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: { preferences: { favoriteComics: [] } },
    });
    mockUseGetComicsQuery.mockReturnValue({
      data: {
        comics: {
          edges: [
            {
              node: {
                id: 1,
                name: 'Comic 1',
                lastStrip: { imageUrl: 'image1.jpg', date: '2024-01-15' },
              },
            },
          ],
        },
      },
      isLoading: false,
    });

    render(<TodaysComics />);

    expect(screen.getByText('View All')).toBeInTheDocument();
  });

  it('handles missing edges in comics data', () => {
    mockUseGetUserPreferencesQuery.mockReturnValue({
      data: { preferences: { favoriteComics: [] } },
    });
    mockUseGetComicsQuery.mockReturnValue({
      data: { comics: {} },
      isLoading: false,
    });

    render(<TodaysComics />);

    expect(screen.getByText('No comics for today')).toBeInTheDocument();
  });
});
