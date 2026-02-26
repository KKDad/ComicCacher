import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
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
    const formattedDate = new Date('2024-01-15').toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
    });
    expect(screen.getByText(formattedDate)).toBeInTheDocument();
  });

  it('renders thumbnail image when provided', () => {
    const comicWithThumbnail = {
      ...mockComic,
      thumbnail: 'https://example.com/thumbnail.jpg',
    };
    render(<ComicTile comic={comicWithThumbnail} />);

    const image = screen.getByRole('img', { name: 'Test Comic' });
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
    expect(link).toHaveAttribute('href', '/comics/1/2024-01-15');
  });

  it('link includes comic ID and date in URL', () => {
    const comic = {
      id: 42,
      name: 'Another Comic',
      date: '2024-12-25',
    };
    render(<ComicTile comic={comic} />);

    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('href', '/comics/42/2024-12-25');
  });

  it('renders New badge', () => {
    render(<ComicTile comic={mockComic} />);
    expect(screen.getByText('New')).toBeInTheDocument();
  });

  it('formats different dates correctly', () => {
    const comic = { ...mockComic, date: '2024-07-04' };
    render(<ComicTile comic={comic} />);
    const formattedDate = new Date('2024-07-04').toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
    });
    expect(screen.getByText(formattedDate)).toBeInTheDocument();
  });
});
