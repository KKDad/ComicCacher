import { cookies } from 'next/headers';
import { NextResponse, type NextRequest } from 'next/server';
import { JWT_COOKIE, REFRESH_COOKIE, COOKIE_MAX_AGE, GRAPHQL_ENDPOINT } from '@/lib/auth/constants';

// Module-level dedup for concurrent refresh attempts
let refreshPromise: Promise<{ token: string; refreshToken: string } | null> | null = null;

async function refreshTokens(refreshToken: string): Promise<{ token: string; refreshToken: string } | null> {
  const res = await fetch(GRAPHQL_ENDPOINT, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      query: `mutation RefreshToken($refreshToken: String!) {
        refreshToken(refreshToken: $refreshToken) {
          token
          refreshToken
        }
      }`,
      variables: { refreshToken },
    }),
  });

  if (!res.ok) return null;

  const json = await res.json();
  const data = json.data?.refreshToken;
  if (!data?.token) return null;

  return { token: data.token, refreshToken: data.refreshToken };
}

async function forwardToBackend(body: string, jwt: string): Promise<Response> {
  return fetch(GRAPHQL_ENDPOINT, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${jwt}`,
    },
    body,
  });
}

function setCookies(response: NextResponse, token: string, refresh: string) {
  response.cookies.set(JWT_COOKIE, token, {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    path: '/',
    maxAge: COOKIE_MAX_AGE,
  });
  response.cookies.set(REFRESH_COOKIE, refresh, {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    path: '/',
    maxAge: COOKIE_MAX_AGE,
  });
}

export async function POST(request: NextRequest) {
  const cookieStore = await cookies();
  const jwt = cookieStore.get(JWT_COOKIE)?.value;

  if (!jwt) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  const body = await request.text();

  // Forward request to backend
  let backendRes = await forwardToBackend(body, jwt);

  // On 401, attempt token refresh
  if (backendRes.status === 401) {
    const refresh = cookieStore.get(REFRESH_COOKIE)?.value;
    if (!refresh) {
      const res = NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
      res.cookies.delete(JWT_COOKIE);
      res.cookies.delete(REFRESH_COOKIE);
      return res;
    }

    // Dedup concurrent refresh requests
    if (!refreshPromise) {
      refreshPromise = refreshTokens(refresh).finally(() => {
        refreshPromise = null;
      });
    }

    const newTokens = await refreshPromise;

    if (!newTokens) {
      const res = NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
      res.cookies.delete(JWT_COOKIE);
      res.cookies.delete(REFRESH_COOKIE);
      return res;
    }

    // Retry the original request with new token
    backendRes = await forwardToBackend(body, newTokens.token);

    // Return response with updated cookies
    const data = await backendRes.json();
    const response = NextResponse.json(data, { status: backendRes.status });
    setCookies(response, newTokens.token, newTokens.refreshToken);
    return response;
  }

  const data = await backendRes.json();
  return NextResponse.json(data, { status: backendRes.status });
}
