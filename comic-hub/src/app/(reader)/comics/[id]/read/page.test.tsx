import { render, screen } from '@testing-library/react';
import { useParams, useSearchParams } from 'next/navigation';
import { useGetUserPreferencesQuery } from '@/generated/graphql';

vi.mock('next/navigation', () => ({
  useParams: vi.fn(),
  useSearchParams: vi.fn(),
}));

vi.mock('@/generated/graphql', () => ({
  useGetUserPreferencesQuery: vi.fn(),
  useGetStripWindowQuery: vi.fn().mockReturnValue({ data: null, isLoading: false }),
  useGetRandomStripQuery: vi.fn().mockReturnValue({ data: null, isLoading: false }),
  useUpdateLastReadMutation: vi.fn().mockReturnValue({ mutate: vi.fn() }),
  useGetComicsQuery: vi.fn().mockReturnValue({ data: null, isLoading: false }),
}));

vi.mock('@/components/reader/comic-reader', () => ({
  ComicReader: ({ comicId, initialDate }: { comicId: number; initialDate?: string }) => (
    <div data-testid="comic-reader" data-comic-id={comicId} data-initial-date={initialDate}>
      ComicReader
    </div>
  ),
}));

vi.mock('@tanstack/react-query', () => ({
  QueryClient: vi.fn(),
  QueryClientProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  useQueryClient: vi.fn().mockReturnValue({
    invalidateQueries: vi.fn(),
  }),
}));

describe('ReaderPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.resetModules();
    vi.mocked(useParams).mockReturnValue({ id: '42' });
    vi.mocked(useSearchParams).mockReturnValue({ get: vi.fn().mockReturnValue(null) } as any);
  });

  it('renders ComicReader with parsed comicId when prefs loaded', async () => {
    vi.mocked(useGetUserPreferencesQuery).mockReturnValue({
      data: { preferences: { lastReadDates: [] } },
      isLoading: false,
    } as any);

    const { default: ReaderPage } = await import('./page');
    render(<ReaderPage />);

    const reader = screen.getByTestId('comic-reader');
    expect(reader).toBeInTheDocument();
    expect(reader).toHaveAttribute('data-comic-id', '42');
  });

  it('shows loading skeleton when prefs are loading and no date param', async () => {
    vi.mocked(useGetUserPreferencesQuery).mockReturnValue({
      data: undefined,
      isLoading: true,
    } as any);

    const { default: ReaderPage } = await import('./page');
    const { container } = render(<ReaderPage />);

    expect(container.querySelectorAll('[data-slot="skeleton"]').length).toBeGreaterThanOrEqual(1);
  });

  it('uses date param over lastReadDate', async () => {
    vi.mocked(useSearchParams).mockReturnValue({ get: vi.fn().mockReturnValue('2026-03-10') } as any);
    vi.mocked(useGetUserPreferencesQuery).mockReturnValue({
      data: {
        preferences: {
          lastReadDates: [{ comicId: 42, date: '2026-01-01' }],
        },
      },
      isLoading: false,
    } as any);

    const { default: ReaderPage } = await import('./page');
    render(<ReaderPage />);

    const reader = screen.getByTestId('comic-reader');
    expect(reader).toHaveAttribute('data-initial-date', '2026-03-10');
  });

  it('uses lastReadDate when no date param', async () => {
    vi.mocked(useGetUserPreferencesQuery).mockReturnValue({
      data: {
        preferences: {
          lastReadDates: [{ comicId: 42, date: '2026-02-15' }],
        },
      },
      isLoading: false,
    } as any);

    const { default: ReaderPage } = await import('./page');
    render(<ReaderPage />);

    const reader = screen.getByTestId('comic-reader');
    expect(reader).toHaveAttribute('data-initial-date', '2026-02-15');
  });

  it('renders ComicReader even when prefs loading if date param exists', async () => {
    vi.mocked(useSearchParams).mockReturnValue({ get: vi.fn().mockReturnValue('2026-03-10') } as any);
    vi.mocked(useGetUserPreferencesQuery).mockReturnValue({
      data: undefined,
      isLoading: true,
    } as any);

    const { default: ReaderPage } = await import('./page');
    render(<ReaderPage />);

    expect(screen.getByTestId('comic-reader')).toBeInTheDocument();
  });
});
