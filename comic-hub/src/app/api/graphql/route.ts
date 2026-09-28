import { cookies } from 'next/headers';
import { NextResponse, type NextRequest } from 'next/server';
import { JWT_COOKIE, REFRESH_COOKIE, REMEMBER_COOKIE, GRAPHQL_ENDPOINT } from '@/lib/auth/constants';
import { refreshTokens, setAuthCookies } from '@/lib/auth/tokens';

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
  setAuthCookies(response.cookies, newTokens.token, newTokens.refreshToken, rememberMe);
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
