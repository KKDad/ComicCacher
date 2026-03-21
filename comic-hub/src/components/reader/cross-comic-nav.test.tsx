import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { CrossComicNav } from './cross-comic-nav';
import { useReadingList } from '@/hooks/use-reading-list';

vi.mock('@/hooks/use-reading-list');

const mockNavigateToComic = vi.fn();

describe('CrossComicNav', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders nothing when no adjacent comics', () => {
    vi.mocked(useReadingList).mockReturnValue({
      comics: [],
      previousComic: null,
      nextComic: null,
      navigateToComic: mockNavigateToComic,
      isLoading: false,
    });

    const { container } = render(<CrossComicNav comicId={1} />);

    expect(container.firstChild).toBeNull();
  });

  it('renders previous comic button when available', () => {
    vi.mocked(useReadingList).mockReturnValue({
      comics: [],
      previousComic: { id: 1, name: 'Archie', avatarUrl: null, lastReadDate: null, newest: null, hasUnread: false },
      nextComic: null,
      navigateToComic: mockNavigateToComic,
      isLoading: false,
    });

    render(<CrossComicNav comicId={2} />);

    expect(screen.getByRole('button', { name: /previous comic: archie/i })).toBeInTheDocument();
  });

  it('renders next comic button when available', () => {
    vi.mocked(useReadingList).mockReturnValue({
      comics: [],
      previousComic: null,
      nextComic: { id: 3, name: 'Zits', avatarUrl: null, lastReadDate: null, newest: null, hasUnread: false },
      navigateToComic: mockNavigateToComic,
      isLoading: false,
    });

    render(<CrossComicNav comicId={2} />);

    expect(screen.getByRole('button', { name: /next comic: zits/i })).toBeInTheDocument();
  });

  it('calls navigateToComic when previous clicked', async () => {
    vi.mocked(useReadingList).mockReturnValue({
      comics: [],
      previousComic: { id: 1, name: 'Archie', avatarUrl: null, lastReadDate: null, newest: null, hasUnread: false },
      nextComic: null,
      navigateToComic: mockNavigateToComic,
      isLoading: false,
    });

    render(<CrossComicNav comicId={2} />);

    await userEvent.click(screen.getByRole('button', { name: /previous comic: archie/i }));
    expect(mockNavigateToComic).toHaveBeenCalledWith(1);
  });

  it('calls navigateToComic when next clicked', async () => {
    vi.mocked(useReadingList).mockReturnValue({
      comics: [],
      previousComic: null,
      nextComic: { id: 3, name: 'Zits', avatarUrl: null, lastReadDate: null, newest: null, hasUnread: false },
      navigateToComic: mockNavigateToComic,
      isLoading: false,
    });

    render(<CrossComicNav comicId={2} />);

    await userEvent.click(screen.getByRole('button', { name: /next comic: zits/i }));
    expect(mockNavigateToComic).toHaveBeenCalledWith(3);
  });
});
