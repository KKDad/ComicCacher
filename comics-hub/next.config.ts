import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async rewrites() {
    return [
      {
        source: '/api/v1/:path*',
        destination: `${process.env.NEXT_PUBLIC_GRAPHQL_ENDPOINT!.replace('/graphql', '')}/api/v1/:path*`,
      },
    ];
  },
};

export default nextConfig;
