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
import { isOperator } from '@/lib/roles';
import { Button } from '@/components/ui/button';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
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

export function NavRail() {
  const pathname = usePathname();
  const { logout, isLoggingOut } = useLogout();
  const user = useUser();
  const showOperations = isOperator(user?.roles ?? []);

  const renderNavItem = (item: { href: string; label: string; icon: React.ComponentType<{ className?: string }> }) => {
    const Icon = item.icon;
    const isActive = pathname === item.href;

    return (
      <Tooltip key={item.href}>
        <TooltipTrigger asChild>
          <Link href={item.href}>
            <Button
              variant={isActive ? 'secondary' : 'ghost'}
              size="sm"
              className={cn(
                'w-full h-12 justify-center',
                isActive && 'bg-primary-subtle text-primary'
              )}
            >
              <Icon className="h-5 w-5" />
            </Button>
          </Link>
        </TooltipTrigger>
        <TooltipContent side="right">
          <p>{item.label}</p>
        </TooltipContent>
      </Tooltip>
    );
  };

  return (
    <aside className="fixed left-0 top-[var(--header-height)] z-sticky h-[calc(100vh-var(--header-height))] w-[var(--sidebar-collapsed)] bg-surface border-r border-border flex flex-col">
      <TooltipProvider delayDuration={0}>
        <nav className="flex-1 overflow-y-auto p-2">
          <div className="space-y-1">
            {baseNavItems.map(renderNavItem)}
          </div>

          {showOperations && (
            <>
              <div className="my-2 border-t border-border" />
              <div className="space-y-1">
                {operationsNavItems.map(renderNavItem)}
              </div>
            </>
          )}
        </nav>

        <div className="p-2 border-t border-border">
          <Tooltip>
            <TooltipTrigger asChild>
              <Button
                variant="ghost"
                size="sm"
                className="w-full h-12 justify-center text-error hover:text-error hover:bg-error-subtle"
                onClick={logout}
                disabled={isLoggingOut}
              >
                <LogOut className="h-5 w-5" />
              </Button>
            </TooltipTrigger>
            <TooltipContent side="right">
              <p>Logout</p>
            </TooltipContent>
          </Tooltip>
        </div>
      </TooltipProvider>
    </aside>
  );
}
