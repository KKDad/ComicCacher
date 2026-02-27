import { render, type RenderOptions } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { UserProvider } from '@/contexts/user-context';
import type { User } from '@/types/auth';

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

export function createMockComic(overrides?: Record<string, unknown>) {
  return {
    id: '1',
    name: 'Test Comic',
    avatarUrl: 'https://example.com/avatar.png',
    author: 'Test Author',
    oldest: '2020-01-01',
    newest: '2024-01-15',
    description: 'A test comic',
    source: 'gocomics',
    lastStrip: {
      date: '2024-01-15',
      imageUrl: 'https://example.com/strip.png',
    },
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

export { createTestQueryClient };
export * from '@testing-library/react';
