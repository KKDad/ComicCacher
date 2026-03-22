import { redirect } from 'next/navigation';
import { getSession } from '@/lib/auth/session';
import { UserProvider } from '@/contexts/user-context';

export default async function ReaderLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const user = await getSession();

  if (!user) {
    redirect('/login');
  }

  return <UserProvider user={user}>{children}</UserProvider>;
}
