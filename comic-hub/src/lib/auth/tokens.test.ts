import { NextResponse } from 'next/server';
import { authCookieOptions, isAccessTokenUsable, refreshTokens, setAuthCookies } from './tokens';

function makeJwt(payload: Record<string, unknown>): string {
  const encode = (value: unknown) => Buffer.from(JSON.stringify(value)).toString('base64url');
  return `${encode({ alg: 'HS256' })}.${encode(payload)}.signature`;
}

const nowSeconds = () => Math.floor(Date.now() / 1000);

describe('isAccessTokenUsable', () => {
  it('accepts a token that expires later', () => {
    expect(isAccessTokenUsable(makeJwt({ exp: nowSeconds() + 600 }))).toBe(true);
  });

  it('rejects an expired token', () => {
    expect(isAccessTokenUsable(makeJwt({ exp: nowSeconds() - 1 }))).toBe(false);
  });

  it('rejects a token expiring inside the skew window', () => {
    expect(isAccessTokenUsable(makeJwt({ exp: nowSeconds() + 10 }))).toBe(false);
    expect(isAccessTokenUsable(makeJwt({ exp: nowSeconds() + 10 }), 5)).toBe(true);
  });

  it('rejects a token without exp', () => {
    expect(isAccessTokenUsable(makeJwt({ sub: 'user' }))).toBe(false);
  });

  it('rejects malformed tokens', () => {
    expect(isAccessTokenUsable('not-a-jwt')).toBe(false);
    expect(isAccessTokenUsable('a.!!!.c')).toBe(false);
  });
});

describe('refreshTokens', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('returns the new tokens', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ data: { refreshToken: { token: 'new-jwt', refreshToken: 'new-refresh' } } })),
    );
    await expect(refreshTokens('old-refresh')).resolves.toEqual({ token: 'new-jwt', refreshToken: 'new-refresh' });
    const body = JSON.parse(vi.mocked(global.fetch).mock.calls[0][1]!.body as string);
    expect(body.variables).toEqual({ refreshToken: 'old-refresh' });
  });

  it('returns null on a non-OK response', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValue(new Response('Server Error', { status: 500 }));
    await expect(refreshTokens('old-refresh')).resolves.toBeNull();
  });

  it('returns null when no token comes back', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValue(new Response(JSON.stringify({ data: { refreshToken: null } })));
    await expect(refreshTokens('old-refresh')).resolves.toBeNull();
  });

  it('returns null when the backend is unreachable', async () => {
    vi.spyOn(global, 'fetch').mockRejectedValue(new TypeError('fetch failed'));
    await expect(refreshTokens('old-refresh')).resolves.toBeNull();
  });
});

describe('authCookieOptions', () => {
  it('persists cookies for remember-me', () => {
    expect(authCookieOptions(true)).toMatchObject({ httpOnly: true, sameSite: 'lax', path: '/', maxAge: 604800 });
  });

  it('uses session cookies otherwise', () => {
    expect(authCookieOptions(false)).not.toHaveProperty('maxAge');
  });
});

describe('setAuthCookies', () => {
  it('sets both auth cookies', () => {
    const response = NextResponse.json({});
    setAuthCookies(response.cookies, 'jwt', 'refresh', true);
    expect(response.cookies.get('comic-hub-jwt')).toMatchObject({ value: 'jwt', maxAge: 604800 });
    expect(response.cookies.get('comic-hub-refresh')).toMatchObject({ value: 'refresh', maxAge: 604800 });
  });
});
