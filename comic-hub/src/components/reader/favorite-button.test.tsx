import { render, screen, fireEvent } from '@testing-library/react';
import { FavoriteButton } from './favorite-button';
import { useFavorite } from '@/hooks/use-favorite';

vi.mock('@/hooks/use-favorite', () => ({
  useFavorite: vi.fn(),
}));

describe('FavoriteButton', () => {
  it('offers to add a comic that is not a favorite', () => {
    const toggle = vi.fn();
    vi.mocked(useFavorite).mockReturnValue({ isFavorite: false, toggle, isPending: false });

    render(<FavoriteButton comicId={1} comicName="Garfield" />);
    const button = screen.getByRole('button', { name: 'Add Garfield to favorites' });

    expect(button).toHaveAttribute('aria-pressed', 'false');
    fireEvent.click(button);
    expect(toggle).toHaveBeenCalledOnce();
  });

  it('shows a pressed button for a favorite', () => {
    vi.mocked(useFavorite).mockReturnValue({ isFavorite: true, toggle: vi.fn(), isPending: false });

    render(<FavoriteButton comicId={1} comicName="Garfield" />);

    expect(screen.getByRole('button', { name: 'Remove Garfield from favorites' })).toHaveAttribute('aria-pressed', 'true');
  });

  it('is disabled while a change is saving', () => {
    vi.mocked(useFavorite).mockReturnValue({ isFavorite: false, toggle: vi.fn(), isPending: true });

    render(<FavoriteButton comicId={1} comicName="Garfield" />);

    expect(screen.getByRole('button')).toBeDisabled();
  });
});
