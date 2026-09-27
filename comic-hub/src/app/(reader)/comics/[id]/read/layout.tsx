import type { Metadata } from 'next';
import { comicTitle } from '@/lib/comic-title';

export async function generateMetadata({ params }: { params: Promise<{ id: string }> }): Promise<Metadata> {
  const { id } = await params;
  const name = await comicTitle(id);
  return name ? { title: name } : {};
}

export default function Layout({ children }: { children: React.ReactNode }) {
  return children;
}
