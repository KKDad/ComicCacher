import { render, screen } from '@testing-library/react';
import { ComicReader } from './comic-reader';
import { useResponsiveNav } from '@/hooks/use-responsive-nav';
import { useReader } from '@/hooks/use-reader';
import { useReadingList } from '@/hooks/use-reading-list';

// jsdom doesn't implement scrollIntoView or IntersectionObserver
HTMLElement.prototype.scrollIntoView = vi.fn();
global.IntersectionObserver = class IntersectionObserver {
  constructor() {}
  observe() {}
  unobserve() {}
  disconnect() {}
} as any;

vi.mock('@/hooks/use-responsive-nav');
vi.mock('@/hooks/use-reader');
vi.mock('@/hooks/use-reading-list');
vi.mock('@/generated/graphql', () => ({
  useGetStripWindowQuery: vi.fn().mockReturnValue({ data: null, isLoading: false }),
  useGetRandomStripQuery: vi.fn().mockReturnValue({ data: null, isLoading: false }),
  useUpdateLastReadMutation: vi.fn().mockReturnValue({ mutate: vi.fn() }),
  useGetComicsQuery: vi.fn().mockReturnValue({ data: null, isLoading: false }),
  useGetUserPreferencesQuery: vi.fn().mockReturnValue({ data: null, isLoading: false }),
}));

const mockReader = {
  strips: [
    { date: '2026-03-15', available: true, imageUrl: 'https://example.com/strip.png', width: 900, height: 300 },
  ],
  currentIndex: 0,
  setCurrentIndex: vi.fn(),
  comicName: 'Garfield',
  oldest: '2020-01-01',
  newest: '2026-03-20',
  avatarUrl: null,
  hasOlder: true,
  hasNewer: true,
  isLoading: false,
  loadOlder: vi.fn(),
  loadNewer: vi.fn(),
  goToDate: vi.fn(),
  goToFirst: vi.fn(),
  goToLast: vi.fn(),
  goToRandom: vi.fn(),
  goNewer: vi.fn(),
  goOlder: vi.fn(),
  isLoadingRandom: false,
};

describe('ComicReader', () => {
  beforeEach(() => {
    vi.mocked(useReader).mockReturnValue(mockReader);
    vi.mocked(useReadingList).mockReturnValue({
      comics: [],
      previousComic: null,
      nextComic: null,
      navigateToComic: vi.fn(),
      isLoading: false,
    });
  });

  it('renders desktop reader on desktop layout', () => {
    vi.mocked(useResponsiveNav).mockReturnValue({
      layout: 'desktop',
      isCollapsed: false,
      toggle: vi.fn(),
    });

    render(<ComicReader comicId={1} />);

    expect(screen.getByText('Garfield')).toBeInTheDocument();
  });

  it('renders mobile reader on mobile layout', () => {
    vi.mocked(useResponsiveNav).mockReturnValue({
      layout: 'mobile',
      isCollapsed: false,
      toggle: vi.fn(),
    });

    render(<ComicReader comicId={1} />);

    expect(screen.getByText('Garfield')).toBeInTheDocument();
  });

  it('passes initialDate to useReader', () => {
    vi.mocked(useResponsiveNav).mockReturnValue({
      layout: 'desktop',
      isCollapsed: false,
      toggle: vi.fn(),
    });

    render(<ComicReader comicId={1} initialDate="2026-03-15" />);

    expect(useReader).toHaveBeenCalledWith(
      expect.objectContaining({ comicId: 1, initialDate: '2026-03-15' }),
    );
  });
});
