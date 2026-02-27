import { getAuthenticatedClient } from './graphql-server';
import { cookies } from 'next/headers';

vi.mock('next/headers', () => ({
  cookies: vi.fn(),
}));

vi.mock('graphql-request', () => ({
  GraphQLClient: class MockGraphQLClient {
    url: string;
    options: Record<string, unknown>;
    constructor(url: string, options: Record<string, unknown>) {
      this.url = url;
      this.options = options;
    }
  },
}));

function mockCookieStore(values: Record<string, string> = {}) {
  const store = {
    get: vi.fn((name: string) => {
      const value = values[name];
      return value ? { name, value } : undefined;
    }),
  };
  vi.mocked(cookies).mockResolvedValue(store as any);
}

describe('getAuthenticatedClient', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('creates client with Bearer header when JWT exists', async () => {
    mockCookieStore({ 'comics-hub-jwt': 'test-jwt' });
    const client = await getAuthenticatedClient() as any;
    expect(client.options.headers).toEqual({ Authorization: 'Bearer test-jwt' });
  });

  it('creates client with empty headers when no JWT', async () => {
    mockCookieStore({});
    const client = await getAuthenticatedClient() as any;
    expect(client.options.headers).toEqual({});
  });
});
