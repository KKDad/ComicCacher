import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ReadingListDrawer } from './reading-list-drawer';
import { useReadingList } from '@/hooks/use-reading-list';

vi.mock('@/hooks/use-reading-list');

const mockNavigateToComic = vi.fn();

describe('ReadingListDrawer', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders trigger button', () => {
    vi.mocked(useReadingList).mockReturnValue({
      comics: [],
      previousComic: null,
      nextComic: null,
      navigateToComic: mockNavigateToComic,
      isLoading: false,
    });

    render(<ReadingListDrawer comicId={1} />);

    expect(screen.getByRole('button', { name: /reading list/i })).toBeInTheDocument();
  });

  it('shows loading state', async () => {
    vi.mocked(useReadingList).mockReturnValue({
      comics: [],
      previousComic: null,
      nextComic: null,
      navigateToComic: mockNavigateToComic,
      isLoading: true,
    });

    render(<ReadingListDrawer comicId={1} />);

    await userEvent.click(screen.getByRole('button', { name: /reading list/i }));
    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  it('shows empty state when no comics', async () => {
    vi.mocked(useReadingList).mockReturnValue({
      comics: [],
      previousComic: null,
      nextComic: null,
      navigateToComic: mockNavigateToComic,
      isLoading: false,
    });

    render(<ReadingListDrawer comicId={1} />);

    await userEvent.click(screen.getByRole('button', { name: /reading list/i }));
    expect(screen.getByText(/no comics in reading list/i)).toBeInTheDocument();
  });

  it('renders comic list items', async () => {
    vi.mocked(useReadingList).mockReturnValue({
      comics: [
        { id: 1, name: 'Garfield', avatarUrl: null, lastReadDate: null, newest: null, hasUnread: false },
        { id: 2, name: 'Zits', avatarUrl: null, lastReadDate: null, newest: null, hasUnread: true },
      ],
      previousComic: null,
      nextComic: null,
      navigateToComic: mockNavigateToComic,
      isLoading: false,
    });

    render(<ReadingListDrawer comicId={1} />);

    await userEvent.click(screen.getByRole('button', { name: /reading list/i }));
    expect(screen.getByText('Garfield')).toBeInTheDocument();
    expect(screen.getByText('Zits')).toBeInTheDocument();
  });
});
