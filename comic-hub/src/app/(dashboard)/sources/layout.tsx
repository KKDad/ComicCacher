import type { Metadata } from 'next';
import { notFound } from 'next/navigation';
import { getSession } from '@/lib/auth/session';
import { isOperator } from '@/lib/roles';

export const metadata: Metadata = { title: 'Sources' };

/**
 * The Sources pages are for operators and admins. The check runs on the server, so a reader who
 * types the URL gets a 404 rather than a page whose every query fails.
 */
export default async function SourcesLayout({ children }: { children: React.ReactNode }) {
  const user = await getSession();
  if (!user || !isOperator(user.roles)) {
    notFound();
  }
  return children;
}
