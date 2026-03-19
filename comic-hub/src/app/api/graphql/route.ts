import { cookies } from 'next/headers';
import { NextResponse, type NextRequest } from 'next/server';
import { JWT_COOKIE, REFRESH_COOKIE, REMEMBER_COOKIE, COOKIE_MAX_AGE, GRAPHQL_ENDPOINT } from '@/lib/auth/constants';

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

function setCookies(response: NextResponse, token: string, refresh: string, rememberMe: boolean) {
  const cookieOptions = {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax' as const,
    path: '/',
    ...(rememberMe ? { maxAge: COOKIE_MAX_AGE } : {}),
  };
  response.cookies.set(JWT_COOKIE, token, cookieOptions);
  response.cookies.set(REFRESH_COOKIE, refresh, cookieOptions);
}

function clearAuthCookies(): NextResponse {
  const res = NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  res.cookies.delete(JWT_COOKIE);
  res.cookies.delete(REFRESH_COOKIE);
  res.cookies.delete(REMEMBER_COOKIE);
  return res;
}

async function attemptRefresh(
  cookieStore: Awaited<ReturnType<typeof cookies>>,
  body: string,
): Promise<NextResponse> {
  const refresh = cookieStore.get(REFRESH_COOKIE)?.value;
  if (!refresh) return clearAuthCookies();

  const newTokens = await refreshTokens(refresh);
  if (!newTokens) return clearAuthCookies();

  const retryRes = await forwardToBackend(body, newTokens.token);
  const retryData = await retryRes.json();
  const rememberMe = cookieStore.get(REMEMBER_COOKIE)?.value === '1';
  const response = NextResponse.json(retryData, { status: retryRes.status });
  setCookies(response, newTokens.token, newTokens.refreshToken, rememberMe);
  return response;
}

export async function POST(request: NextRequest) {
  const cookieStore = await cookies();
  const jwt = cookieStore.get(JWT_COOKIE)?.value;

  if (!jwt) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  const body = await request.text();

  // Forward request to backend
  const backendRes = await forwardToBackend(body, jwt);

  // On 401, attempt token refresh
  if (backendRes.status === 401) {
    return attemptRefresh(cookieStore, body);
  }

  const data = await backendRes.json();

  // Detect GraphQL-level auth errors (backend returns 200 with "Access Denied" errors)
  if (hasAuthError(data)) {
    return attemptRefresh(cookieStore, body);
  }

  return NextResponse.json(data, { status: backendRes.status });
}

function hasAuthError(data: any): boolean {
  if (!data?.errors?.length) return false;
  return data.errors.some(
    (e: any) =>
      e.extensions?.classification === 'UNAUTHORIZED' ||
      e.extensions?.errorCode === 'UNAUTHENTICATED',
  );
}
