import type { Metadata } from 'next';
import { comicTitle } from '@/lib/comic-title';

export async function generateMetadata(
  { params }: { params: Promise<{ id: string }> },
): Promise<Metadata> {
  const { id } = await params;
  return { title: await comicTitle(id) };
}

export default function Layout({ children }: { children: React.ReactNode }) {
  return children;
}
