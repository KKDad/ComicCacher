import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ComicDetailPage from './page';
import { useParams, useRouter } from 'next/navigation';
import { useGetComicQuery } from '@/generated/graphql';

vi.mock('@/generated/graphql', () => ({
  useGetComicQuery: vi.fn(),
}));

const mockRouter = {
  push: vi.fn(),
  replace: vi.fn(),
  prefetch: vi.fn(),
  back: vi.fn(),
  refresh: vi.fn(),
  forward: vi.fn(),
};

const mockComic = {
  id: 1,
  name: 'Garfield',
  description: 'A lazy cat comic',
  author: 'Jim Davis',
  source: 'gocomics',
  oldest: '1978-06-19',
  newest: '2024-01-15',
  avatarUrl: 'https://example.com/garfield.png',
  lastStrip: {
    date: '2024-01-15',
    imageUrl: 'https://example.com/strip.png',
  },
};

describe('ComicDetailPage', () => {
  beforeEach(() => {
    vi.mocked(useParams).mockReturnValue({ id: '1' });
    vi.mocked(useRouter).mockReturnValue(mockRouter);
    vi.mocked(useGetComicQuery).mockReturnValue({
      data: { comic: mockComic },
      isLoading: false,
    } as any);
  });

  afterEach(() => {
    vi.restoreAllMocks();
    Object.values(mockRouter).forEach((fn) => fn.mockClear());
  });

  it('renders loading skeleton when loading', () => {
    vi.mocked(useGetComicQuery).mockReturnValue({ data: null, isLoading: true } as any);
    render(<ComicDetailPage />);
    expect(screen.queryByText('Garfield')).not.toBeInTheDocument();
  });

  it('renders not-found state when comic is null', () => {
    vi.mocked(useGetComicQuery).mockReturnValue({ data: { comic: null }, isLoading: false } as any);
    render(<ComicDetailPage />);
    expect(screen.getByText('Failed to load comic details')).toBeInTheDocument();
  });

  it('renders comic name', () => {
    render(<ComicDetailPage />);
    expect(screen.getByText('Garfield')).toBeInTheDocument();
  });

  it('renders author', () => {
    render(<ComicDetailPage />);
    expect(screen.getByText('Jim Davis')).toBeInTheDocument();
  });

  it('renders year range', () => {
    render(<ComicDetailPage />);
    expect(screen.getByText('1978 - 2024')).toBeInTheDocument();
  });

  it('renders description', () => {
    render(<ComicDetailPage />);
    expect(screen.getByText('A lazy cat comic')).toBeInTheDocument();
  });

  it('renders source', () => {
    render(<ComicDetailPage />);
    expect(screen.getByText(/gocomics/)).toBeInTheDocument();
  });

  it('renders avatar image', () => {
    render(<ComicDetailPage />);
    expect(screen.getByAltText('Garfield')).toHaveAttribute('src', 'https://example.com/garfield.png');
  });

  it('renders latest strip section', () => {
    render(<ComicDetailPage />);
    expect(screen.getByText('Latest Strip')).toBeInTheDocument();
    expect(screen.getByText('Read Latest Strip')).toBeInTheDocument();
  });

  it('renders back link to /comics', () => {
    render(<ComicDetailPage />);
    const backLink = screen.getAllByRole('link').find((l) => l.getAttribute('href') === '/comics');
    expect(backLink).toBeInTheDocument();
  });

  it('clicks Browse Comics button in not-found state', async () => {
    vi.mocked(useGetComicQuery).mockReturnValue({ data: { comic: null }, isLoading: false } as any);
    render(<ComicDetailPage />);
    await userEvent.click(screen.getByRole('button', { name: /browse comics/i }));
    expect(mockRouter.push).toHaveBeenCalledWith('/comics');
  });

  it('renders initial fallback when no avatar', () => {
    vi.mocked(useGetComicQuery).mockReturnValue({
      data: { comic: { ...mockComic, avatarUrl: null } },
      isLoading: false,
    } as any);
    render(<ComicDetailPage />);
    expect(screen.getByText('G')).toBeInTheDocument();
  });
});
