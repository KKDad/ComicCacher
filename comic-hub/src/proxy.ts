import { NextResponse, type NextRequest } from 'next/server';
import { JWT_COOKIE, REFRESH_COOKIE, REMEMBER_COOKIE } from '@/lib/auth/constants';
import { isAccessTokenUsable, refreshTokens, setAuthCookies } from '@/lib/auth/tokens';

// Refreshes an expired access token before a page renders. Server components can't set cookies, so without this
// getSession() in the layouts rejects the stale token and redirects to /login even with a valid refresh token.
// Proxy never redirects: the layouts stay the auth gate, and /api/* handlers refresh on their own.
export async function proxy(request: NextRequest) {
  const refresh = request.cookies.get(REFRESH_COOKIE)?.value;
  const jwt = request.cookies.get(JWT_COOKIE)?.value;
  if (!refresh || (jwt && isAccessTokenUsable(jwt))) return NextResponse.next();

  const tokens = await refreshTokens(refresh);
  if (!tokens) {
    // Refresh token expired or revoked: clear the cookies so the layout's redirect to /login sticks
    const response = NextResponse.next();
    response.cookies.delete(JWT_COOKIE);
    response.cookies.delete(REFRESH_COOKIE);
    response.cookies.delete(REMEMBER_COOKIE);
    return response;
  }

  // Forward the new tokens so cookies() in this render sees them, then send them to the browser
  request.cookies.set(JWT_COOKIE, tokens.token);
  request.cookies.set(REFRESH_COOKIE, tokens.refreshToken);
  const response = NextResponse.next({ request: { headers: request.headers } });
  const rememberMe = request.cookies.get(REMEMBER_COOKIE)?.value === '1';
  setAuthCookies(response.cookies, tokens.token, tokens.refreshToken, rememberMe);
  return response;
}

export const config = {
  matcher: [
    // Pages and their RSC requests only: not /api, build output, metadata files or public/ assets.
    // Prefetches stay matched so a prefetched layout never renders with an expired token.
    '/((?!api/|_next/static|_next/image|favicon.ico|manifest.webmanifest|.*\\.(?:svg|png|jpg|jpeg|gif|webp|ico)$).*)',
  ],
};
