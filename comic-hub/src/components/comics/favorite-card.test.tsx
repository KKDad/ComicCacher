import { render, screen } from '@testing-library/react';
import { FavoriteCard } from './favorite-card';

vi.mock('@/components/ui/avatar', () => ({
  Avatar: ({ children, ...props }: any) => <span data-slot="avatar" {...props}>{children}</span>,
  AvatarImage: (props: any) => <img data-slot="avatar-image" {...props} />,
  AvatarFallback: ({ children, ...props }: any) => <span data-slot="avatar-fallback" {...props}>{children}</span>,
}));

describe('FavoriteCard', () => {
  const comic = { id: 42, name: 'Calvin and Hobbes' };

  it('renders the comic name', () => {
    render(<FavoriteCard comic={comic} />);
    expect(screen.getByText('Calvin and Hobbes')).toBeInTheDocument();
  });

  it('links to the comic detail page', () => {
    render(<FavoriteCard comic={comic} />);
    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('href', '/comics/42');
  });

  it('shows initials from multi-word name', () => {
    render(<FavoriteCard comic={comic} />);
    expect(screen.getByText('CA')).toBeInTheDocument();
  });

  it('shows initials from single-word name', () => {
    render(<FavoriteCard comic={{ id: 1, name: 'Garfield' }} />);
    expect(screen.getByText('G')).toBeInTheDocument();
  });

  it('truncates initials to 2 characters', () => {
    render(<FavoriteCard comic={{ id: 1, name: 'A B C D E' }} />);
    expect(screen.getByText('AB')).toBeInTheDocument();
  });

  it('renders avatar image when avatarUrl is provided', () => {
    render(<FavoriteCard comic={{ id: 1, name: 'Garfield', avatarUrl: 'https://example.com/garfield.png' }} />);
    const img = screen.getByRole('img', { name: 'Garfield' });
    expect(img).toHaveAttribute('src', 'https://example.com/garfield.png');
  });

  it('does not render avatar image when avatarUrl is null', () => {
    render(<FavoriteCard comic={{ id: 1, name: 'Garfield', avatarUrl: null }} />);
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
  });
});
