'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  LayoutDashboard,
  BookOpen,
  BarChart3,
  RefreshCw,
  Code,
  Settings,
  LogOut,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import { useAuth } from '@/hooks/use-auth';

const navItems = [
  { href: '/', label: 'Dashboard', icon: LayoutDashboard },
  { href: '/comics', label: 'Comics List', icon: BookOpen },
  { href: '/metrics', label: 'Metrics', icon: BarChart3 },
  { href: '/retrieval-status', label: 'Retrieval Status', icon: RefreshCw },
  { href: '/api', label: 'API', icon: Code },
  { href: '/preferences', label: 'Preferences', icon: Settings },
];

export function NavRail() {
  const pathname = usePathname();
  const { logout } = useAuth();

  const handleLogout = async () => {
    await logout();
  };

  return (
    <aside className="fixed left-0 top-[var(--header-height)] h-[calc(100vh-var(--header-height))] w-[var(--sidebar-collapsed)] bg-surface border-r border-border flex flex-col">
      <TooltipProvider delayDuration={0}>
        <nav className="flex-1 overflow-y-auto p-2">
          <div className="space-y-1">
            {navItems.map((item) => {
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
            })}
          </div>
        </nav>

        <div className="p-2 border-t border-border">
          <Tooltip>
            <TooltipTrigger asChild>
              <Button
                variant="ghost"
                size="sm"
                className="w-full h-12 justify-center text-error hover:text-error hover:bg-error-subtle"
                onClick={handleLogout}
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
