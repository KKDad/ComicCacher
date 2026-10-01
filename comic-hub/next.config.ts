import type { NextConfig } from "next";

const securityHeaders = [
  { key: 'X-Content-Type-Options', value: 'nosniff' },
  { key: 'X-Frame-Options', value: 'DENY' },
  { key: 'X-XSS-Protection', value: '1; mode=block' },
  { key: 'Referrer-Policy', value: 'strict-origin-when-cross-origin' },
  { key: 'Permissions-Policy', value: 'camera=(), microphone=(), geolocation=()' },
  { key: 'Strict-Transport-Security', value: 'max-age=63072000; includeSubDomains; preload' },
];

const nextConfig: NextConfig = {
  output: 'standalone',
  // The default bottom-left badge covers the sidebar's Sign out button (dev only)
  devIndicators: { position: 'bottom-right' },
  images: {
    // Comic avatars and strips, and the Sources page's catalog thumbnails, served by the backend
    // through the /api/v1 rewrite below. The optimizer caches them for the upstream Cache-Control
    // (1 day avatars, 7 days strips and thumbnails).
    localPatterns: [
      { pathname: '/api/v1/comics/**', search: '' },
      { pathname: '/api/v1/sources/*/thumbnails/*', search: '' },
    ],
  },
  async headers() {
    return [
      {
        source: '/(.*)',
        headers: securityHeaders,
      },
    ];
  },
  async rewrites() {
    return [
      {
        source: '/api/v1/:path*',
        destination: `${(process.env.NEXT_PUBLIC_GRAPHQL_ENDPOINT ?? 'http://localhost:8888/graphql').replace('/graphql', '')}/api/v1/:path*`,
      },
    ];
  },
};

export default nextConfig;
