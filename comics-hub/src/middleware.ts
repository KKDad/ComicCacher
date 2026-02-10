import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

const publicPaths = ['/login', '/register', '/forgot-password'];
const authPaths = ['/login', '/register', '/forgot-password'];

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // Get auth state from cookie or localStorage (stored by zustand persist)
  // Note: We check for the presence of auth-storage in cookies as a marker
  const authCookie = request.cookies.get('auth-storage');
  const isAuthenticated = !!authCookie?.value;

  // Parse auth data from cookie if available
  let hasValidToken = false;
  if (authCookie?.value) {
    try {
      const authData = JSON.parse(authCookie.value);
      hasValidToken = authData?.state?.isAuthenticated && authData?.state?.tokens?.accessToken;
    } catch {
      hasValidToken = false;
    }
  }

  // Redirect authenticated users away from auth pages
  if (hasValidToken && authPaths.includes(pathname)) {
    return NextResponse.redirect(new URL('/', request.url));
  }

  // Redirect unauthenticated users to login (except for public paths)
  if (!hasValidToken && !publicPaths.includes(pathname)) {
    const loginUrl = new URL('/login', request.url);
    loginUrl.searchParams.set('from', pathname);
    return NextResponse.redirect(loginUrl);
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    /*
     * Match all request paths except:
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - favicon.ico (favicon file)
     * - public files (public directory)
     */
    '/((?!_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp)$).*)',
  ],
};
