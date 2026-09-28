import { headers } from 'next/headers';
import { redirect } from 'next/navigation';
import { getSession } from '@/lib/auth/session';
import { PATHNAME_HEADER } from '@/lib/auth/constants';
import { loginPath } from '@/lib/safe-redirect';
import { DashboardShell } from '@/components/layout/dashboard-shell';
import { UserProvider } from '@/contexts/user-context';
import { PreferencesSync } from '@/components/theme/preferences-sync';

export default async function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const user = await getSession();

  if (!user) {
    redirect(loginPath((await headers()).get(PATHNAME_HEADER)));
  }

  return (
    <UserProvider user={user}>
      <PreferencesSync />
      <DashboardShell>{children}</DashboardShell>
    </UserProvider>
  );
}
