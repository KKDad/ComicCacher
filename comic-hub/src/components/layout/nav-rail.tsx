'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { LogOut } from 'lucide-react';
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
import { baseNavItems, operationsNavItems, isNavActive, type NavItem } from './nav-items';

function RailLink({ item, active }: { item: NavItem; active: boolean }) {
  const Icon = item.icon;
  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <Button
          asChild
          variant="ghost"
          className={cn(
            'w-full h-12 justify-center',
            active && 'bg-primary-subtle text-primary hover:bg-primary-subtle hover:text-primary',
          )}
        >
          <Link href={item.href} aria-label={item.label} aria-current={active ? 'page' : undefined}>
            <Icon className="h-5 w-5" aria-hidden="true" />
          </Link>
        </Button>
      </TooltipTrigger>
      <TooltipContent side="right">{item.label}</TooltipContent>
    </Tooltip>
  );
}

export function NavRail() {
  const pathname = usePathname();
  const { logout, isLoggingOut } = useLogout();
  const user = useUser();
  const showOperations = isOperator(user?.roles ?? []);

  return (
    <aside className="fixed left-0 top-[var(--header-height)] z-sticky h-[calc(100dvh-var(--header-height))] w-[var(--sidebar-collapsed)] bg-surface border-r border-border flex flex-col">
      <TooltipProvider delayDuration={0}>
        <nav aria-label="Main" className="flex-1 overflow-y-auto p-2">
          <ul className="space-y-1">
            {baseNavItems.map((item) => (
              <li key={item.href}>
                <RailLink item={item} active={isNavActive(pathname, item.href)} />
              </li>
            ))}
          </ul>

          {showOperations && (
            <ul aria-label="Operations" className="space-y-1 mt-2 pt-2 border-t border-border">
              {operationsNavItems.map((item) => (
                <li key={item.href}>
                  <RailLink item={item} active={isNavActive(pathname, item.href)} />
                </li>
              ))}
            </ul>
          )}
        </nav>

        <div className="p-2 border-t border-border">
          <Tooltip>
            <TooltipTrigger asChild>
              <Button
                variant="ghost"
                className="w-full h-12 justify-center text-error hover:text-error hover:bg-error-subtle"
                onClick={logout}
                disabled={isLoggingOut}
                aria-label="Sign out"
              >
                <LogOut className="h-5 w-5" aria-hidden="true" />
              </Button>
            </TooltipTrigger>
            <TooltipContent side="right">Sign out</TooltipContent>
          </Tooltip>
        </div>
      </TooltipProvider>
    </aside>
  );
}
