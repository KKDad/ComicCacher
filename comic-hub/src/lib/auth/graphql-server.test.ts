import { serverGraphql } from './graphql-server';
import { mockCookieStore } from '@/test/mock-cookies';

vi.mock('next/headers', () => ({
  cookies: vi.fn(),
  headers: vi.fn(),
}));

const QUERY = 'query GetComic($id: Int!) { comic(id: $id) { name } }';

function mockFetch(body: unknown, status = 200) {
  return vi.spyOn(global, 'fetch').mockResolvedValue(new Response(JSON.stringify(body), { status }));
}

function sentRequest() {
  const [, init] = vi.mocked(global.fetch).mock.calls[0];
  return { headers: init!.headers as Record<string, string>, body: JSON.parse(init!.body as string) };
}

describe('serverGraphql', () => {
  beforeEach(() => {
    vi.spyOn(console, 'log').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('sends the query, variables and operation name with the Bearer header when a JWT exists', async () => {
    mockCookieStore({ 'comic-hub-jwt': 'test-jwt' });
    mockFetch({ data: { comic: { name: 'Shoe' } } });

    await expect(serverGraphql(QUERY, { id: 42 })).resolves.toEqual({ comic: { name: 'Shoe' } });

    const { headers, body } = sentRequest();
    expect(headers.Authorization).toBe('Bearer test-jwt');
    expect(headers['x-request-id']).toEqual(expect.any(String));
    expect(body).toEqual({ query: QUERY, variables: { id: 42 }, operationName: 'GetComic' });
  });

  it('sends no Authorization header when there is no JWT', async () => {
    mockCookieStore({});
    mockFetch({ data: { logout: true } });

    await serverGraphql('mutation Logout { logout }');

    expect(sentRequest().headers).not.toHaveProperty('Authorization');
  });

  it('throws on a non-2xx status', async () => {
    mockCookieStore({});
    mockFetch({}, 401);

    await expect(serverGraphql(QUERY)).rejects.toThrow('GraphQL request failed: 401');
  });

  it('throws on a GraphQL error, even with partial data', async () => {
    mockCookieStore({});
    mockFetch({ data: { comic: null }, errors: [{ message: 'Not found' }] });

    await expect(serverGraphql(QUERY)).rejects.toThrow('Not found');
  });
});
