'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { LogOut } from 'lucide-react';
import { cn } from '@/lib/utils';
import { isOperator } from '@/lib/roles';
import { Button } from '@/components/ui/button';
import { useLogout } from '@/hooks/use-auth';
import { useUser } from '@/contexts/user-context';
import { baseNavItems, operationsNavItems, isNavActive, type NavItem } from './nav-items';

function SidebarLink({ item, active }: { item: NavItem; active: boolean }) {
  const Icon = item.icon;
  return (
    <Button
      asChild
      variant="ghost"
      className={cn(
        'w-full justify-start gap-3',
        active && 'bg-primary-subtle text-primary font-medium hover:bg-primary-subtle hover:text-primary',
      )}
    >
      <Link href={item.href} aria-current={active ? 'page' : undefined}>
        <Icon className="h-5 w-5" aria-hidden="true" />
        {item.label}
      </Link>
    </Button>
  );
}

export function Sidebar() {
  const pathname = usePathname();
  const { logout, isLoggingOut } = useLogout();
  const user = useUser();
  const showOperations = isOperator(user?.roles ?? []);

  return (
    <aside className="fixed left-0 top-[var(--header-height)] z-sticky h-[calc(100dvh-var(--header-height))] w-[var(--sidebar-width)] bg-surface border-r border-border flex flex-col">
      <nav aria-label="Main" className="flex-1 overflow-y-auto p-4">
        <ul className="space-y-1">
          {baseNavItems.map((item) => (
            <li key={item.href}>
              <SidebarLink item={item} active={isNavActive(pathname, item.href)} />
            </li>
          ))}
        </ul>

        {showOperations && (
          <div className="mt-6">
            <h2 id="sidebar-ops" className="px-3 mb-2 font-sans text-xs font-semibold text-muted-foreground uppercase tracking-wider">
              Operations
            </h2>
            <ul aria-labelledby="sidebar-ops" className="space-y-1">
              {operationsNavItems.map((item) => (
                <li key={item.href}>
                  <SidebarLink item={item} active={isNavActive(pathname, item.href)} />
                </li>
              ))}
            </ul>
          </div>
        )}
      </nav>

      <div className="p-4 border-t border-border">
        <Button
          variant="ghost"
          className="w-full justify-start gap-3 text-error hover:text-error hover:bg-error-subtle"
          onClick={logout}
          disabled={isLoggingOut}
        >
          <LogOut className="h-5 w-5" aria-hidden="true" />
          {isLoggingOut ? 'Signing out...' : 'Sign out'}
        </Button>
      </div>
    </aside>
  );
}
