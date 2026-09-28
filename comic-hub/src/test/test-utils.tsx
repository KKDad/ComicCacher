import { render, type RenderOptions } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { UserProvider } from '@/contexts/user-context';
import type { User } from '@/types/auth';
import type { GetComicsQuery } from '@/generated/graphql';

export function createMockUser(overrides?: Partial<User>): User {
  return {
    username: 'testuser',
    email: 'test@example.com',
    displayName: 'Test User',
    roles: ['USER'],
    created: '2024-01-01T00:00:00Z',
    ...overrides,
  };
}

function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  });
}

interface ProviderOptions {
  user?: User | null;
  queryClient?: QueryClient;
}

export function renderWithProviders(
  ui: React.ReactElement,
  { user, queryClient, ...renderOptions }: ProviderOptions & Omit<RenderOptions, 'wrapper'> = {},
) {
  const client = queryClient ?? createTestQueryClient();
  const resolvedUser = user === undefined ? createMockUser() : user;

  function Wrapper({ children }: { children: React.ReactNode }) {
    return (
      <QueryClientProvider client={client}>
        <UserProvider user={resolvedUser}>{children}</UserProvider>
      </QueryClientProvider>
    );
  }

  return { ...render(ui, { wrapper: Wrapper, ...renderOptions }), queryClient: client };
}

export * from '@testing-library/react';

/** The original image URL behind a next/image `<img>`, whose `src` points at the optimizer. */
export function imageSrc(img: HTMLElement): string | null {
  const src = img.getAttribute('src');
  if (!src) return null;
  return new URL(src, 'http://localhost').searchParams.get('url') ?? src;
}

/** A comic as `useAllComics` returns it (the `GetComics` node). */
export type ComicNode = GetComicsQuery['comics']['edges'][number]['node'];

/** A comic with empty optional fields, for the ones a test doesn't care about. */
export function mockComic(comic: Partial<ComicNode> & Pick<ComicNode, 'id' | 'name'>): ComicNode {
  return { description: null, oldest: null, newest: null, avatarUrl: null, lastStrip: null, ...comic };
}
