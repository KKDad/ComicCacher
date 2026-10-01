import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient } from '@tanstack/react-query';
import SourcesPage from './page';
import { createMockUser, renderWithProviders } from '@/test/test-utils';
import { mockQueryResult } from '@/test/mock-query';
import { mockSource } from '@/test/source-fixtures';
import { useGetSourcesQuery, type GetSourcesQuery } from '@/generated/graphql';
import { useSourceActions } from '@/hooks/use-source-actions';

vi.mock('@/generated/graphql', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/generated/graphql')>()),
  useGetSourcesQuery: vi.fn(),
}));

vi.mock('@/hooks/use-source-actions', () => ({
  useSourceActions: vi.fn(),
}));

const actions = {
  refreshCatalog: vi.fn(),
  backfillSource: vi.fn(),
};

describe('SourcesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useSourceActions).mockReturnValue(actions as unknown as ReturnType<typeof useSourceActions>);
  });

  it('shows a card per source', () => {
    vi.mocked(useGetSourcesQuery).mockReturnValue(
      mockQueryResult<GetSourcesQuery>({ data: { sources: [mockSource(), mockSource({ id: 'comicskingdom', displayName: 'Comics Kingdom' })] } }),
    );

    renderWithProviders(<SourcesPage />, { user: createMockUser({ roles: ['ADMIN'] }) });

    expect(screen.getByRole('heading', { name: 'Sources' })).toBeInTheDocument();
    expect(screen.getByText('GoComics')).toBeInTheDocument();
    expect(screen.getByText('Comics Kingdom')).toBeInTheDocument();
  });

  it('shows skeletons while loading and an error when it fails', () => {
    vi.mocked(useGetSourcesQuery).mockReturnValue(mockQueryResult<GetSourcesQuery>({ isLoading: true }));
    const { unmount } = renderWithProviders(<SourcesPage />);
    expect(screen.queryByText('GoComics')).not.toBeInTheDocument();
    unmount();

    vi.mocked(useGetSourcesQuery).mockReturnValue(mockQueryResult<GetSourcesQuery>({ error: new Error('boom') }));
    renderWithProviders(<SourcesPage />);
    expect(screen.getByText('Failed to load sources: boom')).toBeInTheDocument();
  });

  it('refreshes a catalog and starts polling', async () => {
    vi.mocked(useGetSourcesQuery).mockReturnValue(mockQueryResult<GetSourcesQuery>({ data: { sources: [mockSource()] } }));

    renderWithProviders(<SourcesPage />, { user: createMockUser({ roles: ['ADMIN'] }) });
    await userEvent.click(screen.getByRole('button', { name: /Refresh catalog/ }));

    expect(actions.refreshCatalog).toHaveBeenCalledWith('gocomics');
    const options = vi.mocked(useGetSourcesQuery).mock.lastCall?.[1];
    const refetchInterval = options?.refetchInterval as (query: { state: { data?: GetSourcesQuery } }) => number | false;
    expect(refetchInterval({ state: { data: { sources: [mockSource()] } } })).toBe(3000);
  });

  it('polls while a refresh runs and stops after', () => {
    vi.mocked(useGetSourcesQuery).mockReturnValue(mockQueryResult<GetSourcesQuery>({ data: { sources: [] } }));
    renderWithProviders(<SourcesPage />, { queryClient: new QueryClient() });

    const options = vi.mocked(useGetSourcesQuery).mock.lastCall?.[1];
    const refetchInterval = options?.refetchInterval as (query: { state: { data?: GetSourcesQuery } }) => number | false;
    expect(refetchInterval({ state: { data: { sources: [mockSource({ refreshing: true })] } } })).toBe(3000);
    expect(refetchInterval({ state: { data: { sources: [mockSource()] } } })).toBe(false);
    expect(refetchInterval({ state: {} })).toBe(false);
  });

  it('lets operators look but not change', () => {
    vi.mocked(useGetSourcesQuery).mockReturnValue(mockQueryResult<GetSourcesQuery>({ data: { sources: [mockSource()] } }));

    renderWithProviders(<SourcesPage />, { user: createMockUser({ roles: ['OPERATOR'] }) });

    expect(screen.queryByRole('button', { name: /Refresh catalog/ })).not.toBeInTheDocument();
  });
});
