import { renderHook, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  useGetUserPreferencesQuery,
  useAddFavoriteMutation,
  useRemoveFavoriteMutation,
} from '@/generated/graphql';
import { useFavorite } from './use-favorite';

vi.mock('@/generated/graphql', () => ({
  useGetUserPreferencesQuery: vi.fn(),
  useAddFavoriteMutation: vi.fn(),
  useRemoveFavoriteMutation: vi.fn(),
}));

type Prefs = ReturnType<typeof useGetUserPreferencesQuery>;
type AddMutation = ReturnType<typeof useAddFavoriteMutation>;
type RemoveMutation = ReturnType<typeof useRemoveFavoriteMutation>;

const addMutate = vi.fn();
const removeMutate = vi.fn();

function setup(favoriteComics: number[]) {
  vi.mocked(useGetUserPreferencesQuery).mockReturnValue({
    data: { preferences: { favoriteComics, lastReadDates: [], username: 'u' } },
  } as unknown as Prefs);
  vi.mocked(useAddFavoriteMutation).mockReturnValue({ mutate: addMutate, isPending: false } as unknown as AddMutation);
  vi.mocked(useRemoveFavoriteMutation).mockReturnValue({ mutate: removeMutate, isPending: false } as unknown as RemoveMutation);

  const queryClient = new QueryClient();
  const invalidate = vi.spyOn(queryClient, 'invalidateQueries');
  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  return { invalidate, wrapper };
}

describe('useFavorite', () => {
  beforeEach(() => vi.clearAllMocks());

  it('adds a comic that is not a favorite', () => {
    const { wrapper } = setup([2]);
    const { result } = renderHook(() => useFavorite(1), { wrapper });

    expect(result.current.isFavorite).toBe(false);
    act(() => result.current.toggle());

    expect(addMutate).toHaveBeenCalledWith({ comicId: 1 });
    expect(removeMutate).not.toHaveBeenCalled();
  });

  it('removes a comic that is a favorite', () => {
    const { wrapper } = setup([1]);
    const { result } = renderHook(() => useFavorite(1), { wrapper });

    expect(result.current.isFavorite).toBe(true);
    act(() => result.current.toggle());

    expect(removeMutate).toHaveBeenCalledWith({ comicId: 1 });
  });

  it('refreshes preferences only after a successful change', () => {
    const { invalidate, wrapper } = setup([]);
    renderHook(() => useFavorite(1), { wrapper });
    const addOptions = vi.mocked(useAddFavoriteMutation).mock.calls[0][0]!;
    const removeOptions = vi.mocked(useRemoveFavoriteMutation).mock.calls[0][0]!;
    type AddResult = Parameters<NonNullable<typeof addOptions.onSuccess>>[0];
    type RemoveResult = Parameters<NonNullable<typeof removeOptions.onSuccess>>[0];
    const ctx = [undefined, undefined, undefined] as unknown as [never, never, never];

    addOptions.onSuccess!({ addFavorite: { errors: [{ message: 'no' }] } } as AddResult, ...ctx);
    expect(invalidate).not.toHaveBeenCalled();

    addOptions.onSuccess!({ addFavorite: { errors: [] } } as unknown as AddResult, ...ctx);
    removeOptions.onSuccess!({ removeFavorite: { errors: [] } } as unknown as RemoveResult, ...ctx);
    expect(invalidate).toHaveBeenCalledTimes(2);
  });
});
