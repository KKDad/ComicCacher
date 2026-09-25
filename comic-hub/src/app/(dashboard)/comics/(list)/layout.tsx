import type { Metadata } from 'next';

export const metadata: Metadata = { title: 'Comics' };

export default function Layout({ children }: { children: React.ReactNode }) {
  return children;
}
