import type { MetadataRoute } from 'next';

// Lets the app be added to a phone's home screen with its own name and icon.
export default function manifest(): MetadataRoute.Manifest {
  return {
    name: 'Comics Hub',
    short_name: 'Comics',
    description: 'Your personal comic strip collection',
    start_url: '/',
    display: 'standalone',
    background_color: '#F2ECDF',
    theme_color: '#F2ECDF',
    icons: [
      { src: '/icon-192.png', sizes: '192x192', type: 'image/png' },
      { src: '/icon-512.png', sizes: '512x512', type: 'image/png' },
    ],
  };
}
