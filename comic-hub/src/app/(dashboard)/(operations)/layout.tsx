import { notFound } from 'next/navigation';
import { getSession } from '@/lib/auth/session';
import { isOperator } from '@/lib/roles';

/**
 * The operations pages (metrics, retrieval status, batch jobs) are for operators and admins. The
 * check runs on the server, so a reader who types the URL gets a 404 rather than a page whose every
 * query fails. The API rejects their queries for readers as well.
 */
export default async function OperationsLayout({ children }: { children: React.ReactNode }) {
  const user = await getSession();
  if (!user || !isOperator(user.roles)) {
    notFound();
  }
  return children;
}
