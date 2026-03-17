import { NextRequest } from 'next/server';
import { cookies } from 'next/headers';

vi.mock('next/headers', () => ({
  cookies: vi.fn(),
}));

// Use dynamic import to reset module-level state between tests
async function importRoute() {
  return import('./route');
}

function createRequest(body = { query: '{ comics { id } }' }) {
  return new NextRequest('http://localhost/api/graphql', {
    method: 'POST',
    body: JSON.stringify(body),
    headers: { 'Content-Type': 'application/json' },
  });
}

function mockCookieStore(values: Record<string, string> = {}) {
  const store = {
    get: vi.fn((name: string) => {
      const value = values[name];
      return value ? { name, value } : undefined;
    }),
  };
  vi.mocked(cookies).mockResolvedValue(store as any);
  return store;
}

describe('POST /api/graphql', () => {
  beforeEach(() => {
    vi.resetModules();
    vi.spyOn(global, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ data: { comics: [] } })),
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('returns 401 when no JWT cookie', async () => {
    mockCookieStore({});
    const { POST } = await importRoute();
    const response = await POST(createRequest());
    expect(response.status).toBe(401);
    const json = await response.json();
    expect(json.error).toBe('Unauthorized');
  });

  it('forwards request with Bearer token', async () => {
    mockCookieStore({ 'comic-hub-jwt': 'test-jwt' });
    const { POST } = await importRoute();
    await POST(createRequest());
    expect(global.fetch).toHaveBeenCalled();
    const [, fetchOptions] = vi.mocked(global.fetch).mock.calls[0];
    expect((fetchOptions!.headers as Record<string, string>).Authorization).toBe('Bearer test-jwt');
  });

  it('returns backend response on success', async () => {
    mockCookieStore({ 'comic-hub-jwt': 'test-jwt' });
    const { POST } = await importRoute();
    const response = await POST(createRequest());
    const json = await response.json();
    expect(json).toEqual({ data: { comics: [] } });
  });

  it('attempts token refresh on 401 from backend', async () => {
    mockCookieStore({
      'comic-hub-jwt': 'expired-jwt',
      'comic-hub-refresh': 'valid-refresh',
    });

    vi.mocked(global.fetch)
      // First call: backend returns 401
      .mockResolvedValueOnce(new Response('Unauthorized', { status: 401 }))
      // Second call: refresh succeeds
      .mockResolvedValueOnce(
        new Response(JSON.stringify({
          data: { refreshToken: { token: 'new-jwt', refreshToken: 'new-refresh' } },
        })),
      )
      // Third call: retry with new token succeeds
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ data: { comics: [{ id: 1 }] } })),
      );

    const { POST } = await importRoute();
    const response = await POST(createRequest());
    const json = await response.json();
    expect(json).toEqual({ data: { comics: [{ id: 1 }] } });
    expect(response.cookies.get('comic-hub-jwt')?.value).toBe('new-jwt');
    expect(response.cookies.get('comic-hub-refresh')?.value).toBe('new-refresh');
  });

  it('returns 401 when no refresh token available', async () => {
    mockCookieStore({ 'comic-hub-jwt': 'expired-jwt' });

    vi.mocked(global.fetch).mockResolvedValueOnce(
      new Response('Unauthorized', { status: 401 }),
    );

    const { POST } = await importRoute();
    const response = await POST(createRequest());
    expect(response.status).toBe(401);
  });

  it('returns 401 when refresh fails', async () => {
    mockCookieStore({
      'comic-hub-jwt': 'expired-jwt',
      'comic-hub-refresh': 'invalid-refresh',
    });

    vi.mocked(global.fetch)
      .mockResolvedValueOnce(new Response('Unauthorized', { status: 401 }))
      .mockResolvedValueOnce(new Response('Server Error', { status: 500 }));

    const { POST } = await importRoute();
    const response = await POST(createRequest());
    expect(response.status).toBe(401);
  });

  it('detects GraphQL Access Denied and attempts refresh', async () => {
    mockCookieStore({
      'comic-hub-jwt': 'expired-jwt',
      'comic-hub-refresh': 'valid-refresh',
    });

    vi.mocked(global.fetch)
      // First call: backend returns 200 with Access Denied GraphQL error
      .mockResolvedValueOnce(
        new Response(JSON.stringify({
          errors: [{ message: 'Access Denied', extensions: { classification: 'UNAUTHORIZED' } }],
          data: { batchSchedulers: null },
        })),
      )
      // Second call: refresh succeeds
      .mockResolvedValueOnce(
        new Response(JSON.stringify({
          data: { refreshToken: { token: 'new-jwt', refreshToken: 'new-refresh' } },
        })),
      )
      // Third call: retry with new token succeeds
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ data: { batchSchedulers: [] } })),
      );

    const { POST } = await importRoute();
    const response = await POST(createRequest());
    const json = await response.json();
    expect(json).toEqual({ data: { batchSchedulers: [] } });
    expect(response.cookies.get('comic-hub-jwt')?.value).toBe('new-jwt');
  });

  it('returns 401 on GraphQL Access Denied when no refresh token', async () => {
    mockCookieStore({ 'comic-hub-jwt': 'expired-jwt' });

    vi.mocked(global.fetch).mockResolvedValueOnce(
      new Response(JSON.stringify({
        errors: [{ message: 'Access Denied' }],
        data: { preferences: null },
      })),
    );

    const { POST } = await importRoute();
    const response = await POST(createRequest());
    expect(response.status).toBe(401);
  });

  it('returns 401 on GraphQL Access Denied when refresh fails', async () => {
    mockCookieStore({
      'comic-hub-jwt': 'expired-jwt',
      'comic-hub-refresh': 'bad-refresh',
    });

    vi.mocked(global.fetch)
      .mockResolvedValueOnce(
        new Response(JSON.stringify({
          errors: [{ message: 'Access Denied' }],
          data: null,
        })),
      )
      .mockResolvedValueOnce(new Response('Server Error', { status: 500 }));

    const { POST } = await importRoute();
    const response = await POST(createRequest());
    expect(response.status).toBe(401);
  });

  it('returns 401 when refresh returns no token', async () => {
    mockCookieStore({
      'comic-hub-jwt': 'expired-jwt',
      'comic-hub-refresh': 'valid-refresh',
    });

    vi.mocked(global.fetch)
      .mockResolvedValueOnce(new Response('Unauthorized', { status: 401 }))
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ data: { refreshToken: null } })),
      );

    const { POST } = await importRoute();
    const response = await POST(createRequest());
    expect(response.status).toBe(401);
  });
});
