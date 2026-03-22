import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { CrossComicNav } from './cross-comic-nav';

const mockNavigateToComic = vi.fn();

describe('CrossComicNav', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders nothing when no adjacent comics', () => {
    const { container } = render(
      <CrossComicNav previousComic={null} nextComic={null} navigateToComic={mockNavigateToComic} />,
    );

    expect(container.firstChild).toBeNull();
  });

  it('renders previous comic button when available', () => {
    render(
      <CrossComicNav
        previousComic={{ id: 1, name: 'Archie' }}
        nextComic={null}
        navigateToComic={mockNavigateToComic}
      />,
    );

    expect(screen.getByRole('button', { name: /previous comic: archie/i })).toBeInTheDocument();
  });

  it('renders next comic button when available', () => {
    render(
      <CrossComicNav
        previousComic={null}
        nextComic={{ id: 3, name: 'Zits' }}
        navigateToComic={mockNavigateToComic}
      />,
    );

    expect(screen.getByRole('button', { name: /next comic: zits/i })).toBeInTheDocument();
  });

  it('calls navigateToComic when previous clicked', async () => {
    render(
      <CrossComicNav
        previousComic={{ id: 1, name: 'Archie' }}
        nextComic={null}
        navigateToComic={mockNavigateToComic}
      />,
    );

    await userEvent.click(screen.getByRole('button', { name: /previous comic: archie/i }));
    expect(mockNavigateToComic).toHaveBeenCalledWith(1);
  });

  it('calls navigateToComic when next clicked', async () => {
    render(
      <CrossComicNav
        previousComic={null}
        nextComic={{ id: 3, name: 'Zits' }}
        navigateToComic={mockNavigateToComic}
      />,
    );

    await userEvent.click(screen.getByRole('button', { name: /next comic: zits/i }));
    expect(mockNavigateToComic).toHaveBeenCalledWith(3);
  });
});
