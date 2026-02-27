import { fetcher } from './graphql-client';

describe('fetcher', () => {
  const originalLocation = window.location;

  beforeEach(() => {
    vi.spyOn(global, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ data: { comics: [] } })),
    );
    // Mock window.location for 401 redirect test
    Object.defineProperty(window, 'location', {
      writable: true,
      value: { ...originalLocation, href: '' },
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
    Object.defineProperty(window, 'location', {
      writable: true,
      value: originalLocation,
    });
  });

  it('returns data on success', async () => {
    const result = await fetcher<{ comics: [] }, Record<string, never>>('query { comics }')();
    expect(result).toEqual({ comics: [] });
  });

  it('sends query and variables to /api/graphql', async () => {
    await fetcher('query { comics }', { first: 10 })();
    expect(global.fetch).toHaveBeenCalledWith('/api/graphql', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query: 'query { comics }', variables: { first: 10 } }),
    });
  });

  it('merges custom headers', async () => {
    await fetcher('query { comics }', undefined, { 'X-Custom': 'value' })();
    expect(global.fetch).toHaveBeenCalledWith('/api/graphql', expect.objectContaining({
      headers: { 'Content-Type': 'application/json', 'X-Custom': 'value' },
    }));
  });

  it('redirects to /login on 401', async () => {
    vi.mocked(global.fetch).mockResolvedValue(new Response('Unauthorized', { status: 401 }));
    await expect(fetcher('query { me }')()).rejects.toThrow('Request failed: 401');
    expect(window.location.href).toBe('/login');
  });

  it('throws on non-ok response', async () => {
    vi.mocked(global.fetch).mockResolvedValue(new Response('Server Error', { status: 500 }));
    await expect(fetcher('query { comics }')()).rejects.toThrow('Request failed: 500');
  });

  it('throws on GraphQL errors when no data', async () => {
    vi.mocked(global.fetch).mockResolvedValue(
      new Response(JSON.stringify({ errors: [{ message: 'Field not found' }] })),
    );
    await expect(fetcher('query { bad }')()).rejects.toThrow('Field not found');
  });

  it('returns data even when errors are present alongside data', async () => {
    vi.mocked(global.fetch).mockResolvedValue(
      new Response(JSON.stringify({ data: { comics: [] }, errors: [{ message: 'warning' }] })),
    );
    const result = await fetcher<{ comics: [] }, Record<string, never>>('query { comics }')();
    expect(result).toEqual({ comics: [] });
  });
});
