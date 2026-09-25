import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ComicTile } from './comic-tile';

describe('ComicTile', () => {
  const mockComic = {
    id: 1,
    name: 'Test Comic',
    date: '2024-01-15',
  };

  it('renders comic name', () => {
    render(<ComicTile comic={mockComic} />);
    expect(screen.getByText('Test Comic')).toBeInTheDocument();
  });

  it('renders formatted date', () => {
    render(<ComicTile comic={mockComic} />);
    expect(screen.getByText('Jan 15')).toBeInTheDocument();
  });

  it('renders thumbnail image when provided', () => {
    const comicWithThumbnail = {
      ...mockComic,
      thumbnail: 'https://example.com/thumbnail.jpg',
    };
    render(<ComicTile comic={comicWithThumbnail} />);

    const image = screen.getByRole('presentation');
    expect(image).toBeInTheDocument();
    expect(image).toHaveAttribute('src', 'https://example.com/thumbnail.jpg');
  });

  it('renders fallback initial when no thumbnail provided', () => {
    render(<ComicTile comic={mockComic} />);

    expect(screen.queryByRole('img')).not.toBeInTheDocument();
    expect(screen.getByText('T')).toBeInTheDocument();
  });

  it('renders fallback initial with first character of name', () => {
    const comic = { ...mockComic, name: 'Xkcd' };
    render(<ComicTile comic={comic} />);
    expect(screen.getByText('X')).toBeInTheDocument();
  });

  it('link navigates to correct comic URL', () => {
    render(<ComicTile comic={mockComic} />);

    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('href', '/comics/1/read?date=2024-01-15');
  });

  it('link includes comic ID and date in URL', () => {
    const comic = {
      id: 42,
      name: 'Another Comic',
      date: '2024-12-25',
    };
    render(<ComicTile comic={comic} />);

    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('href', '/comics/42/read?date=2024-12-25');
  });

  it('renders New badge when isNew is true', () => {
    render(<ComicTile comic={mockComic} isNew />);
    expect(screen.getByText('New')).toBeInTheDocument();
  });

  it('does not render New badge when isNew is false', () => {
    render(<ComicTile comic={mockComic} isNew={false} />);
    expect(screen.queryByText('New')).not.toBeInTheDocument();
  });

  it('does not render New badge when isNew is not provided', () => {
    render(<ComicTile comic={mockComic} />);
    expect(screen.queryByText('New')).not.toBeInTheDocument();
  });

  it('renders heart button when onToggleFavorite is provided', () => {
    const handler = vi.fn();
    render(<ComicTile comic={mockComic} onToggleFavorite={handler} />);
    expect(screen.getByRole('button', { name: 'Add Test Comic to favorites' })).toBeInTheDocument();
  });

  it('renders filled heart when isFavorite', () => {
    const handler = vi.fn();
    render(<ComicTile comic={mockComic} isFavorite onToggleFavorite={handler} />);
    expect(screen.getByRole('button', { name: 'Remove Test Comic from favorites' })).toBeInTheDocument();
  });

  it('does not render heart button when onToggleFavorite is not provided', () => {
    render(<ComicTile comic={mockComic} />);
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('calls onToggleFavorite when heart is clicked', async () => {
    const handler = vi.fn();
    render(<ComicTile comic={mockComic} onToggleFavorite={handler} />);
    await userEvent.click(screen.getByRole('button', { name: 'Add Test Comic to favorites' }));
    expect(handler).toHaveBeenCalledOnce();
  });

  it('formats different dates correctly', () => {
    const comic = { ...mockComic, date: '2024-07-04' };
    render(<ComicTile comic={comic} />);
    expect(screen.getByText('Jul 4')).toBeInTheDocument();
  });

  it('makes the title the card link and keeps the favorite button outside it', () => {
    const { container } = render(<ComicTile comic={mockComic} onToggleFavorite={vi.fn()} />);
    expect(screen.getByRole('link', { name: 'Test Comic' })).toBeInTheDocument();
    expect(container.querySelector('a button')).toBeNull();
  });

  it('exposes favorite state with aria-pressed', () => {
    render(<ComicTile comic={mockComic} isFavorite onToggleFavorite={vi.fn()} />);
    expect(screen.getByRole('button', { name: 'Remove Test Comic from favorites' })).toHaveAttribute('aria-pressed', 'true');
  });
});
