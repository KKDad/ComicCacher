import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ComicStripPage from './page';
import { useParams, useRouter } from 'next/navigation';
import { useGetComicStripQuery, useGetComicQuery, useUpdateLastReadMutation } from '@/generated/graphql';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

vi.mock('@/generated/graphql', () => ({
  useGetComicStripQuery: vi.fn(),
  useGetComicQuery: vi.fn(),
  useUpdateLastReadMutation: vi.fn(),
}));

const mockRouter = {
  push: vi.fn(),
  replace: vi.fn(),
  prefetch: vi.fn(),
  back: vi.fn(),
  refresh: vi.fn(),
  forward: vi.fn(),
};

const mockStrip = {
  available: true,
  imageUrl: 'https://example.com/strip.png',
  date: '2024-01-15',
  previous: { date: '2024-01-14' },
  next: { date: '2024-01-16' },
};

const mockMutate = vi.fn();

function renderWithQuery(ui: React.ReactElement) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={qc}>{ui}</QueryClientProvider>);
}

describe('ComicStripPage', () => {
  beforeEach(() => {
    vi.mocked(useParams).mockReturnValue({ id: '1', date: '2024-01-15' });
    vi.mocked(useRouter).mockReturnValue(mockRouter);
    vi.mocked(useGetComicStripQuery).mockReturnValue({
      data: { strip: mockStrip },
      isLoading: false,
    } as any);
    vi.mocked(useGetComicQuery).mockReturnValue({
      data: { comic: { name: 'Garfield' } },
      isLoading: false,
    } as any);
    vi.mocked(useUpdateLastReadMutation).mockReturnValue({ mutate: mockMutate } as any);
  });

  afterEach(() => {
    vi.restoreAllMocks();
    Object.values(mockRouter).forEach((fn) => fn.mockClear());
    mockMutate.mockClear();
  });

  it('renders loading skeleton when loading', () => {
    vi.mocked(useGetComicStripQuery).mockReturnValue({ data: null, isLoading: true } as any);
    renderWithQuery(<ComicStripPage />);
    expect(screen.queryByText('Garfield')).not.toBeInTheDocument();
  });

  it('renders not-found state when strip is null', () => {
    vi.mocked(useGetComicStripQuery).mockReturnValue({ data: { strip: null }, isLoading: false } as any);
    renderWithQuery(<ComicStripPage />);
    expect(screen.getByText('Failed to load comic strip')).toBeInTheDocument();
  });

  it('clicks Go Back button in not-found state', async () => {
    vi.mocked(useGetComicStripQuery).mockReturnValue({ data: { strip: null }, isLoading: false } as any);
    renderWithQuery(<ComicStripPage />);
    await userEvent.click(screen.getByRole('button', { name: /go back/i }));
    expect(mockRouter.back).toHaveBeenCalledOnce();
  });

  it('renders unavailable strip message', () => {
    vi.mocked(useGetComicStripQuery).mockReturnValue({
      data: { strip: { ...mockStrip, available: false, imageUrl: null } },
      isLoading: false,
    } as any);
    renderWithQuery(<ComicStripPage />);
    expect(screen.getByText(/no strip available/i)).toBeInTheDocument();
  });

  it('navigates to previous strip from unavailable state', async () => {
    vi.mocked(useGetComicStripQuery).mockReturnValue({
      data: { strip: { ...mockStrip, available: false, imageUrl: null } },
      isLoading: false,
    } as any);
    renderWithQuery(<ComicStripPage />);
    await userEvent.click(screen.getByRole('button', { name: /previous/i }));
    expect(mockRouter.push).toHaveBeenCalledWith('/comics/1/2024-01-14');
  });

  it('navigates to next strip from unavailable state', async () => {
    vi.mocked(useGetComicStripQuery).mockReturnValue({
      data: { strip: { ...mockStrip, available: false, imageUrl: null } },
      isLoading: false,
    } as any);
    renderWithQuery(<ComicStripPage />);
    await userEvent.click(screen.getByRole('button', { name: /next/i }));
    expect(mockRouter.push).toHaveBeenCalledWith('/comics/1/2024-01-16');
  });

  it('navigates to comic details from unavailable state', async () => {
    vi.mocked(useGetComicStripQuery).mockReturnValue({
      data: { strip: { ...mockStrip, available: false, imageUrl: null } },
      isLoading: false,
    } as any);
    renderWithQuery(<ComicStripPage />);
    await userEvent.click(screen.getByRole('button', { name: /view comic details/i }));
    expect(mockRouter.push).toHaveBeenCalledWith('/comics/1');
  });

  it('renders strip image', () => {
    renderWithQuery(<ComicStripPage />);
    expect(screen.getByAltText(/garfield/i)).toHaveAttribute('src', 'https://example.com/strip.png');
  });

  it('renders comic name', () => {
    renderWithQuery(<ComicStripPage />);
    expect(screen.getByText('Garfield')).toBeInTheDocument();
  });

  it('renders formatted date', () => {
    renderWithQuery(<ComicStripPage />);
    const expected = new Date('2024-01-15').toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
    expect(screen.getByText(expected)).toBeInTheDocument();
  });

  it('renders previous and next buttons', () => {
    renderWithQuery(<ComicStripPage />);
    expect(screen.getByRole('button', { name: /previous/i })).not.toBeDisabled();
    expect(screen.getByRole('button', { name: /next/i })).not.toBeDisabled();
  });

  it('navigates to previous strip on click', async () => {
    renderWithQuery(<ComicStripPage />);
    await userEvent.click(screen.getByRole('button', { name: /previous/i }));
    expect(mockRouter.push).toHaveBeenCalledWith('/comics/1/2024-01-14');
  });

  it('navigates to next strip on click', async () => {
    renderWithQuery(<ComicStripPage />);
    await userEvent.click(screen.getByRole('button', { name: /next/i }));
    expect(mockRouter.push).toHaveBeenCalledWith('/comics/1/2024-01-16');
  });

  it('disables previous button when no previous', () => {
    vi.mocked(useGetComicStripQuery).mockReturnValue({
      data: { strip: { ...mockStrip, previous: null } },
      isLoading: false,
    } as any);
    renderWithQuery(<ComicStripPage />);
    expect(screen.getByRole('button', { name: /previous/i })).toBeDisabled();
  });

  it('disables next button when no next', () => {
    vi.mocked(useGetComicStripQuery).mockReturnValue({
      data: { strip: { ...mockStrip, next: null } },
      isLoading: false,
    } as any);
    renderWithQuery(<ComicStripPage />);
    expect(screen.getByRole('button', { name: /next/i })).toBeDisabled();
  });

  it('renders View Details link', () => {
    renderWithQuery(<ComicStripPage />);
    expect(screen.getByRole('link', { name: /view details/i })).toHaveAttribute('href', '/comics/1');
  });
});
