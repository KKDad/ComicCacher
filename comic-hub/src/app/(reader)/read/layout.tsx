import type { Metadata } from 'next';

export const metadata: Metadata = { title: 'Daily reader' };

export default function Layout({ children }: { children: React.ReactNode }) {
  return children;
}
