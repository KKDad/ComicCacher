import { NextRequest } from 'next/server';
import { unstable_doesMiddlewareMatch } from 'next/experimental/testing/server';
import { config, proxy } from './proxy';

function makeJwt(payload: Record<string, unknown>): string {
  const encode = (value: unknown) => Buffer.from(JSON.stringify(value)).toString('base64url');
  return `${encode({ alg: 'HS256' })}.${encode(payload)}.signature`;
}

const nowSeconds = () => Math.floor(Date.now() / 1000);
const validJwt = makeJwt({ exp: nowSeconds() + 600 });
const expiredJwt = makeJwt({ exp: nowSeconds() - 60 });

function createRequest(cookies: Record<string, string> = {}) {
  const cookie = Object.entries(cookies).map(([name, value]) => `${name}=${value}`).join('; ');
  return new NextRequest('http://localhost/comics/1/read', { headers: cookie ? { cookie } : {} });
}

function refreshSucceeds() {
  vi.mocked(global.fetch).mockResolvedValue(
    new Response(JSON.stringify({ data: { refreshToken: { token: 'new-jwt', refreshToken: 'new-refresh' } } })),
  );
}

// The cookie header proxy forwards to the render, or undefined when it didn't override it
function forwardedCookieHeader(response: Response) {
  return response.headers.get('x-middleware-request-cookie') ?? undefined;
}

describe('proxy', () => {
  beforeEach(() => {
    vi.spyOn(global, 'fetch');
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('passes through without a refresh cookie', async () => {
    const response = await proxy(createRequest({ 'comic-hub-jwt': expiredJwt }));
    expect(global.fetch).not.toHaveBeenCalled();
    expect(response.cookies.getAll()).toEqual([]);
    expect(forwardedCookieHeader(response)).toBeUndefined();
  });

  it('passes through while the access token is still valid', async () => {
    const response = await proxy(createRequest({ 'comic-hub-jwt': validJwt, 'comic-hub-refresh': 'refresh' }));
    expect(global.fetch).not.toHaveBeenCalled();
    expect(response.cookies.getAll()).toEqual([]);
  });

  it('refreshes an expired access token and forwards the new one to the render', async () => {
    refreshSucceeds();
    const response = await proxy(createRequest({ 'comic-hub-jwt': expiredJwt, 'comic-hub-refresh': 'old-refresh' }));

    const body = JSON.parse(vi.mocked(global.fetch).mock.calls[0][1]!.body as string);
    expect(body.variables).toEqual({ refreshToken: 'old-refresh' });
    expect(response.cookies.get('comic-hub-jwt')?.value).toBe('new-jwt');
    expect(response.cookies.get('comic-hub-refresh')?.value).toBe('new-refresh');
    expect(forwardedCookieHeader(response)).toContain('comic-hub-jwt=new-jwt');
    expect(forwardedCookieHeader(response)).toContain('comic-hub-refresh=new-refresh');
  });

  it('refreshes when the access cookie is missing', async () => {
    refreshSucceeds();
    const response = await proxy(createRequest({ 'comic-hub-refresh': 'old-refresh' }));
    expect(response.cookies.get('comic-hub-jwt')?.value).toBe('new-jwt');
  });

  it('keeps remember-me sessions persistent', async () => {
    refreshSucceeds();
    const response = await proxy(createRequest({
      'comic-hub-jwt': expiredJwt,
      'comic-hub-refresh': 'old-refresh',
      'comic-hub-remember': '1',
    }));
    expect(response.cookies.get('comic-hub-jwt')?.maxAge).toBe(604800);
    expect(response.cookies.get('comic-hub-refresh')?.maxAge).toBe(604800);
  });

  it('sets session cookies without remember-me', async () => {
    refreshSucceeds();
    const response = await proxy(createRequest({ 'comic-hub-jwt': expiredJwt, 'comic-hub-refresh': 'old-refresh' }));
    expect(response.cookies.get('comic-hub-jwt')?.maxAge).toBeUndefined();
    expect(response.cookies.get('comic-hub-refresh')?.maxAge).toBeUndefined();
  });

  it('clears the auth cookies when the refresh is rejected', async () => {
    vi.mocked(global.fetch).mockResolvedValue(new Response(JSON.stringify({ data: { refreshToken: null } })));
    const response = await proxy(createRequest({
      'comic-hub-jwt': expiredJwt,
      'comic-hub-refresh': 'revoked',
      'comic-hub-remember': '1',
    }));
    for (const name of ['comic-hub-jwt', 'comic-hub-refresh', 'comic-hub-remember']) {
      expect(response.cookies.get(name)?.value).toBe('');
    }
    expect(forwardedCookieHeader(response)).toBeUndefined();
  });

  it('clears the auth cookies when the backend is unreachable', async () => {
    vi.mocked(global.fetch).mockRejectedValue(new TypeError('fetch failed'));
    const response = await proxy(createRequest({ 'comic-hub-jwt': expiredJwt, 'comic-hub-refresh': 'old-refresh' }));
    expect(response.cookies.get('comic-hub-jwt')?.value).toBe('');
    expect(response.cookies.get('comic-hub-refresh')?.value).toBe('');
  });
});

describe('proxy matcher', () => {
  it.each(['/', '/comics/1/read', '/login', '/admin/users'])('runs on %s', (url) => {
    expect(unstable_doesMiddlewareMatch({ config, url })).toBe(true);
  });

  it.each([
    '/api/graphql',
    '/api/v1/comics/1/avatar',
    '/_next/static/chunks/app.js',
    '/_next/image?url=x',
    '/favicon.ico',
    '/icon.svg',
    '/icon-192.png',
    '/apple-icon.png',
    '/manifest.webmanifest',
  ])('skips %s', (url) => {
    expect(unstable_doesMiddlewareMatch({ config, url })).toBe(false);
  });
});
