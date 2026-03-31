'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  LayoutDashboard,
  BookOpen,
  Newspaper,
  BarChart3,
  RefreshCw,
  Code,
  Settings,
  LogOut,
  Cog,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { isOperator, isAdmin } from '@/lib/roles';
import { Button } from '@/components/ui/button';
import { useLogout } from '@/hooks/use-auth';
import { useUser } from '@/contexts/user-context';

const baseNavItems = [
  { href: '/', label: 'Dashboard', icon: LayoutDashboard },
  { href: '/read', label: 'Daily Reader', icon: Newspaper },
  { href: '/comics', label: 'Comics List', icon: BookOpen },
  { href: '/api', label: 'API', icon: Code },
  { href: '/preferences', label: 'Preferences', icon: Settings },
];

const operationsNavItems = [
  { href: '/metrics', label: 'Metrics', icon: BarChart3 },
  { href: '/retrieval-status', label: 'Retrieval Status', icon: RefreshCw },
  { href: '/batch-jobs', label: 'Batch Jobs', icon: Cog },
];

export function Sidebar() {
  const pathname = usePathname();
  const { logout, isLoggingOut } = useLogout();
  const user = useUser();
  const showOperations = isOperator(user?.roles ?? []);

  return (
    <aside className="fixed left-0 top-[var(--header-height)] z-sticky h-[calc(100vh-var(--header-height))] w-[var(--sidebar-width)] bg-surface border-r border-border flex flex-col">
      <nav className="flex-1 overflow-y-auto p-4">
        <div className="space-y-1">
          {baseNavItems.map((item) => {
            const Icon = item.icon;
            const isActive = pathname === item.href;

            return (
              <Link key={item.href} href={item.href}>
                <Button
                  variant={isActive ? 'secondary' : 'ghost'}
                  className={cn(
                    'w-full justify-start gap-3',
                    isActive && 'bg-primary-subtle text-primary font-medium'
                  )}
                >
                  <Icon className="h-5 w-5" />
                  {item.label}
                </Button>
              </Link>
            );
          })}
        </div>

        {showOperations && (
          <div className="mt-6">
            <div className="px-3 mb-2 text-xs font-semibold text-muted-foreground uppercase tracking-wider">
              Operations
            </div>
            <div className="space-y-1">
              {operationsNavItems.map((item) => {
                const Icon = item.icon;
                const isActive = pathname === item.href;

                return (
                  <Link key={item.href} href={item.href}>
                    <Button
                      variant={isActive ? 'secondary' : 'ghost'}
                      className={cn(
                        'w-full justify-start gap-3',
                        isActive && 'bg-primary-subtle text-primary font-medium'
                      )}
                    >
                      <Icon className="h-5 w-5" />
                      {item.label}
                    </Button>
                  </Link>
                );
              })}
            </div>
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
          <LogOut className="h-5 w-5" />
          {isLoggingOut ? 'Signing out...' : 'Logout'}
        </Button>
      </div>
    </aside>
  );
}
