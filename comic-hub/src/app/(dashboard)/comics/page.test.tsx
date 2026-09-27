import { render, screen, fireEvent, within } from '@testing-library/react';
import ComicsPage from './page';
import { useSearchComicsQuery } from '@/generated/graphql';
import { useAllComics } from '@/hooks/use-all-comics';
import { useFavorites } from '@/hooks/use-favorite';

vi.mock('@/generated/graphql', () => ({
  useSearchComicsQuery: vi.fn(),
}));

vi.mock('@/hooks/use-all-comics', () => ({
  useAllComics: vi.fn(),
}));

vi.mock('@/hooks/use-favorite', () => ({
  useFavorites: vi.fn(),
}));

const mockSearchParams = new Map<string, string>();
vi.mock('next/navigation', () => ({
  useSearchParams: () => ({
    get: (key: string) => mockSearchParams.get(key) ?? null,
  }),
}));

type AllComics = ReturnType<typeof useAllComics>;
type Search = ReturnType<typeof useSearchComicsQuery>;

function comic(id: number, name: string, extra: Record<string, unknown> = {}) {
  return { id, name, newest: '2024-01-15', avatarUrl: null, lastStrip: null, ...extra };
}

const catalogue = [
  comic(1, 'BC'),
  comic(2, 'Baby Blues', { lastStrip: { date: '2024-01-14', imageUrl: 'https://example.com/bb.png' } }),
  comic(3, 'adam at home'),
];

const mockToggle = vi.fn();

function mockCatalogue(comics: unknown[], isLoading = false) {
  vi.mocked(useAllComics).mockReturnValue({ comics, isLoading, error: null } as unknown as AllComics);
}

function mockSearch(result: unknown, isLoading = false) {
  vi.mocked(useSearchComicsQuery).mockReturnValue({ data: result, isLoading } as unknown as Search);
}

const tileNames = () =>
  within(screen.getByRole('list'))
    .getAllByRole('heading', { level: 3 })
    .map((h) => h.textContent);

describe('ComicsPage', () => {
  beforeEach(() => {
    mockSearchParams.clear();
    mockToggle.mockClear();
    mockCatalogue(catalogue);
    mockSearch(undefined);
    vi.mocked(useFavorites).mockReturnValue({ favoriteIds: new Set([2]), toggle: mockToggle, isPending: false });
  });

  it('renders loading skeletons while the catalogue loads', () => {
    mockCatalogue([], true);
    const { container } = render(<ComicsPage />);
    expect(container.querySelectorAll('[data-slot="skeleton"]').length).toBeGreaterThan(0);
  });

  it('renders the empty state when there are no comics', () => {
    mockCatalogue([]);
    render(<ComicsPage />);
    expect(screen.getByText('No comics available')).toBeInTheDocument();
  });

  it('lists every comic sorted by name, ignoring case', () => {
    render(<ComicsPage />);
    expect(screen.getByText('Explore all 3 available comics')).toBeInTheDocument();
    expect(tileNames()).toEqual(['adam at home', 'Baby Blues', 'BC']);
  });

  it('links each tile to its latest strip', () => {
    render(<ComicsPage />);
    expect(screen.getByRole('link', { name: 'Baby Blues' })).toHaveAttribute('href', '/comics/2/read?date=2024-01-14');
  });

  it('filters the list by name', () => {
    render(<ComicsPage />);

    fireEvent.change(screen.getByRole('searchbox', { name: 'Filter comics by name' }), { target: { value: 'b' } });

    expect(tileNames()).toEqual(['Baby Blues', 'BC']);
  });

  it('says so when nothing matches the filter', () => {
    render(<ComicsPage />);

    fireEvent.change(screen.getByRole('searchbox', { name: 'Filter comics by name' }), { target: { value: 'zzz' } });

    expect(screen.getByText(/No comics match/)).toBeInTheDocument();
    expect(screen.queryByRole('list')).not.toBeInTheDocument();
  });

  it('shows and toggles favorites from the tiles', () => {
    render(<ComicsPage />);

    expect(screen.getByRole('button', { name: 'Remove Baby Blues from favorites' })).toHaveAttribute('aria-pressed', 'true');
    fireEvent.click(screen.getByRole('button', { name: 'Add BC to favorites' }));

    expect(mockToggle).toHaveBeenCalledWith(1);
  });
});

describe('ComicsPage - search mode', () => {
  beforeEach(() => {
    mockSearchParams.set('q', 'garfield');
    mockToggle.mockClear();
    mockCatalogue([]);
    vi.mocked(useFavorites).mockReturnValue({ favoriteIds: new Set(), toggle: mockToggle, isPending: false });
  });

  afterEach(() => {
    mockSearchParams.clear();
  });

  it('renders search results when query param is present', () => {
    mockSearch({ search: { comics: [comic(1, 'Garfield')] } });

    render(<ComicsPage />);
    expect(screen.getByText('Search Results')).toBeInTheDocument();
    expect(screen.getByText(/1 comic matching "garfield"/)).toBeInTheDocument();
    expect(screen.getByText('Garfield')).toBeInTheDocument();
  });

  it('lets a search result be favorited', () => {
    mockSearch({ search: { comics: [comic(7, 'Garfield')] } });

    render(<ComicsPage />);
    fireEvent.click(screen.getByRole('button', { name: 'Add Garfield to favorites' }));

    expect(mockToggle).toHaveBeenCalledWith(7);
  });

  it('shows empty state when search has no results', () => {
    mockSearch({ search: { comics: [] } });

    render(<ComicsPage />);
    expect(screen.getByText('No comics found')).toBeInTheDocument();
    expect(screen.getByText(/No comics matching "garfield"/)).toBeInTheDocument();
  });

  it('shows loading state during search', () => {
    mockSearch(undefined, true);

    render(<ComicsPage />);
    expect(screen.queryByText('Search Results')).not.toBeInTheDocument();
  });

  it('pluralizes result count correctly', () => {
    mockSearch({ search: { comics: [comic(1, 'Garfield'), comic(2, 'Garfield Minus Garfield')] } });

    render(<ComicsPage />);
    expect(screen.getByText(/2 comics matching "garfield"/)).toBeInTheDocument();
  });
});
