import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

// Proxy is a passthrough — auth redirects are handled by server layouts
// (dashboard layout validates session via getSession, not cookie presence).
// Kept for future non-auth proxy use (geo-routing, A/B testing, etc.).
export function proxy(_request: NextRequest) {
  return NextResponse.next();
}

export const config = {
  matcher: [
    /*
     * Match all request paths except:
     * - api/ (API routes — auth boundary is in route handlers)
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - favicon.ico (favicon file)
     * - public files (public directory)
     */
    '/((?!api/|_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp)$).*)',
  ],
};
