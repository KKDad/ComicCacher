import { getAuthenticatedClient } from './graphql-server';
import { mockCookieStore } from '@/test/mock-cookies';

vi.mock('next/headers', () => ({
  cookies: vi.fn(),
}));

// The options each GraphQLClient was constructed with
const clientOptions = vi.hoisted(() => [] as RequestInit[]);

vi.mock('graphql-request', () => ({
  GraphQLClient: class MockGraphQLClient {
    constructor(_url: string, options: RequestInit) {
      clientOptions.push(options);
    }
  },
}));

describe('getAuthenticatedClient', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('creates client with Bearer header when JWT exists', async () => {
    mockCookieStore({ 'comic-hub-jwt': 'test-jwt' });
    await getAuthenticatedClient();
    expect(clientOptions.at(-1)?.headers).toEqual({ Authorization: 'Bearer test-jwt' });
  });

  it('creates client with empty headers when no JWT', async () => {
    mockCookieStore({});
    await getAuthenticatedClient();
    expect(clientOptions.at(-1)?.headers).toEqual({});
  });
});
