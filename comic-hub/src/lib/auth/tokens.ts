import type { NextResponse } from 'next/server';
import { JWT_COOKIE, REFRESH_COOKIE, COOKIE_MAX_AGE, GRAPHQL_ENDPOINT } from './constants';
import { RefreshTokenDocument } from '@/generated/graphql';

// Shared by the /api route handlers and proxy.ts, so not 'server-only'

export async function refreshTokens(refreshToken: string): Promise<{ token: string; refreshToken: string } | null> {
  try {
    const res = await fetch(GRAPHQL_ENDPOINT, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        query: RefreshTokenDocument.toString(),
        variables: { refreshToken },
      }),
    });

    if (!res.ok) return null;

    const json = await res.json();
    const data = json.data?.refreshToken;
    if (!data?.token) return null;

    return { token: data.token, refreshToken: data.refreshToken };
  } catch {
    return null;
  }
}

export function authCookieOptions(rememberMe: boolean) {
  return {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax' as const,
    path: '/',
    ...(rememberMe ? { maxAge: COOKIE_MAX_AGE } : {}),
  };
}

export function setAuthCookies(cookies: NextResponse['cookies'], token: string, refresh: string, rememberMe: boolean) {
  const options = authCookieOptions(rememberMe);
  cookies.set(JWT_COOKIE, token, options);
  cookies.set(REFRESH_COOKIE, refresh, options);
}

/**
 * True when the JWT's `exp` is more than `skewSeconds` away. Reads the payload only; the backend verifies the
 * signature on every call, so this just decides whether a refresh is worth trying.
 */
export function isAccessTokenUsable(jwt: string, skewSeconds = 30): boolean {
  try {
    const payload = jwt.split('.')[1];
    if (!payload) return false;
    const { exp } = JSON.parse(Buffer.from(payload, 'base64url').toString('utf8'));
    if (typeof exp !== 'number') return false;
    return exp - skewSeconds > Date.now() / 1000;
  } catch {
    return false;
  }
}
