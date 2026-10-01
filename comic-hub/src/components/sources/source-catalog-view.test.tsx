import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { SourceCatalogView, PAGE_SIZE } from './source-catalog-view';
import { createMockUser, renderWithProviders } from '@/test/test-utils';
import { mockCatalogSource, mockEntry, mockSourceComic } from '@/test/source-fixtures';
import { useSourceCatalog } from '@/hooks/use-source-catalog';
import { useSourceActions } from '@/hooks/use-source-actions';
import { useCatalogThumbnails } from '@/hooks/use-catalog-thumbnails';
import type { CatalogEntry, SourceCatalog, SourceComic } from '@/types/sources';

vi.mock('@/hooks/use-source-catalog', () => ({ useSourceCatalog: vi.fn() }));
vi.mock('@/hooks/use-source-actions', () => ({ useSourceActions: vi.fn() }));
vi.mock('@/hooks/use-catalog-thumbnails', () => ({ useCatalogThumbnails: vi.fn() }));

const actions = {
  addComic: vi.fn(),
  updateComic: vi.fn(),
  refreshCatalog: vi.fn(),
  backfillSource: vi.fn(),
  backfillComic: vi.fn(),
  fetchAvatar: vi.fn(),
  detectStart: vi.fn(),
  isSaving: false,
  isRefreshing: false,
};
const markBusy = vi.fn();

const configured = mockEntry({
  identifier: 'calvinandhobbes',
  name: 'Calvin and Hobbes',
  author: 'Bill Watterson',
  comic: mockSourceComic(),
});
const notConfigured = mockEntry();
const removed = mockEntry({ identifier: 'gone', name: 'Gone Comic', author: null, removedAt: '2026-09-01T00:00:00Z' });

function loaded(entries: CatalogEntry[], overrides: Partial<SourceCatalog> = {}, orphans: SourceComic[] = []) {
  const source = mockCatalogSource(entries, { orphans, ...overrides });
  vi.mocked(useSourceCatalog).mockReturnValue({
    source,
    entries,
    orphans,
    error: null,
    notFound: false,
    isLoading: false,
    markBusy,
  });
}

function renderView(roles = ['ADMIN']) {
  return renderWithProviders(<SourceCatalogView sourceId="gocomics" />, { user: createMockUser({ roles }) });
}

function row(name: string) {
  return screen.getAllByRole('listitem').find((li) => within(li).queryByText(name)) as HTMLElement;
}

describe('SourceCatalogView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useSourceActions).mockReturnValue(actions as unknown as ReturnType<typeof useSourceActions>);
  });

  it('lists the catalog with counts per filter', () => {
    loaded([configured, notConfigured, removed]);

    renderView();

    expect(screen.getByRole('heading', { name: 'GoComics' })).toBeInTheDocument();
    expect(screen.getAllByRole('listitem')).toHaveLength(3);
    expect(screen.getByRole('button', { name: 'All 3' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'Configured 1' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Not configured 1' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'No longer listed 1' })).toBeInTheDocument();
    expect(within(row('Gone Comic')).getByText('No longer listed')).toBeInTheDocument();
    expect(within(row('Calvin and Hobbes')).getByText('Starts Nov 18, 1985')).toBeInTheDocument();
  });

  it('filters and searches', async () => {
    loaded([configured, notConfigured, removed]);
    renderView();

    await userEvent.click(screen.getByRole('button', { name: 'Not configured 1' }));
    expect(screen.getAllByRole('listitem')).toHaveLength(1);
    expect(screen.getByText('Peanuts')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'All 3' }));
    await userEvent.type(screen.getByRole('searchbox', { name: 'Search the catalog' }), 'watterson');
    expect(screen.getAllByRole('listitem')).toHaveLength(1);
    expect(screen.getByText('Calvin and Hobbes')).toBeInTheDocument();

    await userEvent.clear(screen.getByRole('searchbox'));
    await userEvent.type(screen.getByRole('searchbox'), 'nothing like this');
    expect(screen.getByText('No comics match.')).toBeInTheDocument();
  });

  it('shows genre chips that filter the list, and the description on hover', async () => {
    const funny = mockEntry({ identifier: 'funny', name: 'Funny', description: 'A family of jokers', tags: ['Humor', 'Family'] });
    const drama = mockEntry({ identifier: 'drama', name: 'Drama', tags: ['Soap'] });
    loaded([funny, drama, notConfigured]);
    renderView();

    expect(screen.getByRole('combobox', { name: 'Genre' })).toHaveTextContent('Every genre');
    expect(within(row('Drama')).queryByRole('button', { name: /About/ })).not.toBeInTheDocument();

    await userEvent.hover(within(row('Funny')).getByRole('button', { name: 'About Funny' }));
    expect(await screen.findByRole('tooltip')).toHaveTextContent('A family of jokers');

    await userEvent.click(within(row('Funny')).getByRole('button', { name: 'Humor' }));
    expect(screen.getAllByRole('listitem')).toHaveLength(1);
    expect(screen.getByRole('combobox', { name: 'Genre' })).toHaveTextContent('Humor');
    expect(screen.getByRole('button', { name: 'Humor' })).toHaveAttribute('aria-pressed', 'true');

    // Clicking the chosen chip again shows every genre
    await userEvent.click(screen.getByRole('button', { name: 'Humor' }));
    expect(screen.getAllByRole('listitem')).toHaveLength(3);
  });

  it('searches descriptions and hides the genre filter when nothing has tags', async () => {
    loaded([mockEntry({ identifier: 'funny', name: 'Funny', description: 'A family of jokers' }), notConfigured]);
    renderView();

    expect(screen.queryByRole('combobox', { name: 'Genre' })).not.toBeInTheDocument();
    await userEvent.type(screen.getByRole('searchbox', { name: 'Search the catalog' }), 'jokers');
    expect(screen.getAllByRole('listitem')).toHaveLength(1);
    expect(screen.getByText('Funny')).toBeInTheDocument();
  });

  it('pages through a long catalog and asks for thumbnails of the page shown', async () => {
    const many = Array.from({ length: PAGE_SIZE + 5 }, (_, i) => mockEntry({ identifier: `c${i}`, name: `Comic ${String(i).padStart(3, '0')}` }));
    loaded(many);
    renderView();

    expect(screen.getAllByRole('listitem')).toHaveLength(PAGE_SIZE);
    expect(screen.getByText(`1–${PAGE_SIZE} of ${PAGE_SIZE + 5}`)).toBeInTheDocument();
    expect(vi.mocked(useCatalogThumbnails).mock.lastCall?.[1]).toHaveLength(PAGE_SIZE);

    await userEvent.click(screen.getByRole('button', { name: 'Next' }));
    expect(screen.getAllByRole('listitem')).toHaveLength(5);
    expect(vi.mocked(useCatalogThumbnails).mock.lastCall?.[1]).toHaveLength(5);
    await userEvent.click(screen.getByRole('button', { name: 'Previous' }));
    expect(screen.getAllByRole('listitem')).toHaveLength(PAGE_SIZE);
  });

  it('adds a comic when either switch is turned on', async () => {
    loaded([notConfigured]);
    renderView();

    await userEvent.click(screen.getByRole('switch', { name: 'Download Peanuts' }));
    expect(actions.addComic).toHaveBeenCalledWith('gocomics', 'peanuts', true, true);

    await userEvent.click(screen.getByRole('switch', { name: 'Show Peanuts to readers' }));
    expect(actions.addComic).toHaveBeenCalledWith('gocomics', 'peanuts', false, true);
    expect(markBusy).toHaveBeenCalled();
  });

  it('switches a configured comic off and hides it', async () => {
    loaded([configured]);
    renderView();

    await userEvent.click(screen.getByRole('switch', { name: 'Download Calvin and Hobbes' }));
    expect(actions.updateComic).toHaveBeenCalledWith(1, { active: false });

    await userEvent.click(screen.getByRole('switch', { name: 'Show Calvin and Hobbes to readers' }));
    expect(actions.updateComic).toHaveBeenCalledWith(1, { enabled: false });
  });

  it('runs the row actions for a configured comic', async () => {
    loaded([configured]);
    renderView();

    await userEvent.click(screen.getByRole('button', { name: 'More for Calvin and Hobbes' }));
    await userEvent.click(screen.getByRole('menuitem', { name: /Backfill this comic/ }));
    expect(actions.backfillComic).toHaveBeenCalledWith(1);

    await userEvent.click(screen.getByRole('button', { name: 'More for Calvin and Hobbes' }));
    await userEvent.click(screen.getByRole('menuitem', { name: /Fetch avatar/ }));
    expect(actions.fetchAvatar).toHaveBeenCalledWith(1);
  });

  it('edits the start date in a dialog', async () => {
    loaded([configured]);
    renderView();

    await userEvent.click(screen.getByRole('button', { name: 'More for Calvin and Hobbes' }));
    await userEvent.click(screen.getByRole('menuitem', { name: /Start date/ }));
    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByText('Where Calvin and Hobbes starts')).toBeInTheDocument();

    await userEvent.click(within(dialog).getByRole('button', { name: /Ask the source/ }));
    expect(actions.detectStart).toHaveBeenCalledWith(1);

    const input = within(dialog).getByLabelText('First strip date');
    await userEvent.clear(input);
    await userEvent.type(input, '1985-11-01');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));
    expect(actions.updateComic).toHaveBeenCalledWith(1, { sourceStartDate: '1985-11-01' });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('lets operators look but not change', () => {
    loaded([configured, notConfigured]);
    renderView(['OPERATOR']);

    expect(screen.getByRole('switch', { name: 'Download Peanuts' })).toBeDisabled();
    expect(screen.queryByRole('button', { name: /Refresh catalog/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'More for Calvin and Hobbes' })).not.toBeInTheDocument();
  });

  it('refreshes the catalog', async () => {
    loaded([configured]);
    renderView();

    await userEvent.click(screen.getByRole('button', { name: /Refresh catalog/ }));

    expect(actions.refreshCatalog).toHaveBeenCalledWith('gocomics');
    expect(markBusy).toHaveBeenCalled();
  });

  it('shows a refresh in progress and the last error', () => {
    loaded([configured], { refreshing: true });
    const { unmount } = renderView();
    expect(screen.getByRole('button', { name: /Refreshing/ })).toBeDisabled();
    unmount();

    loaded([configured], { lastRefreshError: 'HTTP 503' });
    renderView();
    expect(screen.getByText('Last refresh failed: HTTP 503')).toBeInTheDocument();
  });

  it('lists configured comics the catalog no longer has', async () => {
    const orphan = mockSourceComic({ id: 9, name: 'Lost Comic', sourceIdentifier: 'lost' });
    loaded([configured], {}, [orphan]);
    renderView();

    const orphans = screen.getByRole('list', { name: 'Comics not in the catalog' });
    expect(within(orphans).getByText('Lost Comic')).toBeInTheDocument();
    await userEvent.click(within(orphans).getByRole('switch', { name: 'Download Lost Comic' }));
    expect(actions.updateComic).toHaveBeenCalledWith(9, { active: false });
  });

  it('explains an unread catalog', () => {
    loaded([]);
    renderView();
    expect(screen.getByText(/hasn't been read yet/)).toHaveTextContent('Use Refresh catalog to read it now.');
  });

  it('shows loading, errors and unknown sources', () => {
    vi.mocked(useSourceCatalog).mockReturnValue({ source: undefined, entries: [], orphans: [], error: null, notFound: false, isLoading: true, markBusy });
    const { unmount } = renderView();
    expect(screen.queryByRole('heading')).not.toBeInTheDocument();
    unmount();

    vi.mocked(useSourceCatalog).mockReturnValue({ source: undefined, entries: [], orphans: [], error: new Error('boom'), notFound: false, isLoading: false, markBusy });
    const second = renderView();
    expect(screen.getByText('Failed to load the catalog: boom')).toBeInTheDocument();
    second.unmount();

    vi.mocked(useSourceCatalog).mockReturnValue({ source: undefined, entries: [], orphans: [], error: null, notFound: true, isLoading: false, markBusy });
    renderView();
    expect(screen.getByText('No source called "gocomics".')).toBeInTheDocument();
  });
});
