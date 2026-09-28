import { NextResponse, type NextRequest } from 'next/server';
import { JWT_COOKIE, PATHNAME_HEADER, REFRESH_COOKIE, REMEMBER_COOKIE } from '@/lib/auth/constants';
import { isAccessTokenUsable, refreshTokens, setAuthCookies } from '@/lib/auth/tokens';
import { REQUEST_ID_HEADER, resolveRequestId } from '@/lib/server-log';

// Refreshes an expired access token before a page renders. Server components can't set cookies, so without this
// getSession() in the layouts rejects the stale token and redirects to /login even with a valid refresh token.
// Proxy never redirects: the layouts stay the auth gate, and /api/* handlers refresh on their own.
// It also passes the requested path to the render, so a layout's redirect to /login can come back to it, and a request
// id that every API call of the render sends, so the render's lines in both logs can be matched.
export async function proxy(request: NextRequest) {
  request.headers.set(PATHNAME_HEADER, requestedPath(request));
  const requestId = resolveRequestId(request.headers.get(REQUEST_ID_HEADER));
  request.headers.set(REQUEST_ID_HEADER, requestId);
  const forward = () => NextResponse.next({ request: { headers: request.headers } });

  const refresh = request.cookies.get(REFRESH_COOKIE)?.value;
  const jwt = request.cookies.get(JWT_COOKIE)?.value;
  if (!refresh || (jwt && isAccessTokenUsable(jwt))) return forward();

  const tokens = await refreshTokens(refresh, requestId);
  if (!tokens) {
    // Refresh token expired or revoked: clear the cookies so the layout's redirect to /login sticks
    const response = forward();
    response.cookies.delete(JWT_COOKIE);
    response.cookies.delete(REFRESH_COOKIE);
    response.cookies.delete(REMEMBER_COOKIE);
    return response;
  }

  // Forward the new tokens so cookies() in this render sees them, then send them to the browser
  request.cookies.set(JWT_COOKIE, tokens.token);
  request.cookies.set(REFRESH_COOKIE, tokens.refreshToken);
  const response = forward();
  const rememberMe = request.cookies.get(REMEMBER_COOKIE)?.value === '1';
  setAuthCookies(response.cookies, tokens.token, tokens.refreshToken, rememberMe);
  return response;
}

// The page path and query, without the RSC request marker
function requestedPath(request: NextRequest): string {
  const url = request.nextUrl.clone();
  url.searchParams.delete('_rsc');
  return url.pathname + url.search;
}

export const config = {
  matcher: [
    // Pages and their RSC requests only: not /api, build output, metadata files or public/ assets.
    // Prefetches stay matched so a prefetched layout never renders with an expired token.
    '/((?!api/|_next/static|_next/image|favicon.ico|manifest.webmanifest|.*\\.(?:svg|png|jpg|jpeg|gif|webp|ico)$).*)',
  ],
};
