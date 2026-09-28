import { cookies } from 'next/headers';
import { NextResponse, type NextRequest } from 'next/server';
import { JWT_COOKIE, REFRESH_COOKIE, REMEMBER_COOKIE, GRAPHQL_ENDPOINT } from '@/lib/auth/constants';
import { refreshTokens, setAuthCookies } from '@/lib/auth/tokens';
import { REQUEST_ID_HEADER, resolveRequestId, timedGraphqlFetch } from '@/lib/server-log';

async function forwardToBackend(body: string, jwt: string, requestId: string): Promise<Response> {
  return timedGraphqlFetch(GRAPHQL_ENDPOINT, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${jwt}`,
    },
    body,
  }, requestId);
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
  requestId: string,
): Promise<NextResponse> {
  const refresh = cookieStore.get(REFRESH_COOKIE)?.value;
  if (!refresh) return clearAuthCookies();

  const newTokens = await refreshTokens(refresh, requestId);
  if (!newTokens) return clearAuthCookies();

  const retryRes = await forwardToBackend(body, newTokens.token, requestId);
  const retryData = await retryRes.json();
  const rememberMe = cookieStore.get(REMEMBER_COOKIE)?.value === '1';
  const response = NextResponse.json(retryData, { status: retryRes.status });
  setAuthCookies(response.cookies, newTokens.token, newTokens.refreshToken, rememberMe);
  return response;
}

// Echoes the request id so the browser's devtools show the id to look for in the logs
export async function POST(request: NextRequest) {
  const requestId = resolveRequestId(request.headers.get(REQUEST_ID_HEADER));
  const response = await handle(request, requestId);
  response.headers.set(REQUEST_ID_HEADER, requestId);
  return response;
}

async function handle(request: NextRequest, requestId: string): Promise<NextResponse> {
  const cookieStore = await cookies();
  const jwt = cookieStore.get(JWT_COOKIE)?.value;

  if (!jwt) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  const body = await request.text();

  // Forward request to backend
  const backendRes = await forwardToBackend(body, jwt, requestId);

  // On 401, attempt token refresh
  if (backendRes.status === 401) {
    return attemptRefresh(cookieStore, body, requestId);
  }

  const data = await backendRes.json();

  // Detect GraphQL-level auth errors (backend returns 200 with "Access Denied" errors)
  if (hasAuthError(data)) {
    return attemptRefresh(cookieStore, body, requestId);
  }

  return NextResponse.json(data, { status: backendRes.status });
}

interface GraphQLErrorBody {
  errors?: { extensions?: { classification?: string; errorCode?: string } }[];
}

function hasAuthError(data: GraphQLErrorBody | null): boolean {
  if (!data?.errors?.length) return false;
  return data.errors.some(
    (e) =>
      e.extensions?.classification === 'UNAUTHORIZED' ||
      e.extensions?.errorCode === 'UNAUTHENTICATED',
  );
}
