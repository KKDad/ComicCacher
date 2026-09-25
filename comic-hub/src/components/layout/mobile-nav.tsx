'use client';

import { useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Menu } from 'lucide-react';
import { cn } from '@/lib/utils';
import { isOperator } from '@/lib/roles';
import { Button } from '@/components/ui/button';
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet';
import { Separator } from '@/components/ui/separator';
import { useLogout } from '@/hooks/use-auth';
import { useUser } from '@/contexts/user-context';
import { baseNavItems, operationsNavItems, isNavActive, type NavItem } from './nav-items';

const bottomNavItems = baseNavItems.filter((item) => item.shortLabel);
const menuItems = baseNavItems.filter((item) => !item.shortLabel);

const barItemClass =
  'flex flex-col items-center justify-center flex-1 h-full min-h-11 gap-1 transition-colors';

export function MobileNav() {
  const pathname = usePathname();
  const [isOpen, setIsOpen] = useState(false);
  const { logout, isLoggingOut } = useLogout();
  const user = useUser();
  const showOperations = isOperator(user?.roles ?? []);

  const handleLogout = async () => {
    setIsOpen(false);
    await logout();
  };

  const renderMenuLink = (item: NavItem) => {
    const active = isNavActive(pathname, item.href);
    return (
      <li key={item.href}>
        <Button
          asChild
          variant="ghost"
          className={cn('w-full justify-start text-base h-12', active && 'text-primary')}
        >
          <Link
            href={item.href}
            aria-current={active ? 'page' : undefined}
            onClick={() => setIsOpen(false)}
          >
            {item.label}
          </Link>
        </Button>
      </li>
    );
  };

  return (
    <nav
      aria-label="Main"
      className="fixed bottom-0 left-0 right-0 bg-surface border-t border-border z-fixed pb-[env(safe-area-inset-bottom)]"
    >
      <div className="flex items-center justify-around h-[var(--mobile-nav-height)]">
        {bottomNavItems.map((item) => {
          const Icon = item.icon;
          const active = isNavActive(pathname, item.href);

          return (
            <Link
              key={item.href}
              href={item.href}
              aria-current={active ? 'page' : undefined}
              className={cn(barItemClass, active ? 'text-primary' : 'text-ink-subtle hover:text-ink')}
            >
              <Icon className="h-5 w-5" aria-hidden="true" />
              <span className="text-xs font-medium">{item.shortLabel}</span>
            </Link>
          );
        })}

        <Sheet open={isOpen} onOpenChange={setIsOpen}>
          <SheetTrigger asChild>
            <button type="button" className={cn(barItemClass, 'text-ink-subtle hover:text-ink')}>
              <Menu className="h-5 w-5" aria-hidden="true" />
              <span className="text-xs font-medium">More</span>
            </button>
          </SheetTrigger>
          <SheetContent side="bottom" className="rounded-t-xl pb-[env(safe-area-inset-bottom)]">
            <SheetHeader>
              <SheetTitle>Menu</SheetTitle>
            </SheetHeader>
            <ul className="mt-2 space-y-1 px-2">
              {menuItems.map(renderMenuLink)}
            </ul>

            {showOperations && (
              <>
                <Separator className="my-2" />
                <h2 id="mobile-ops" className="px-4 py-1 font-sans text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                  Operations
                </h2>
                <ul aria-labelledby="mobile-ops" className="space-y-1 px-2">
                  {operationsNavItems.map(renderMenuLink)}
                </ul>
              </>
            )}

            <Separator className="my-2" />
            <div className="px-2 pb-4">
              <Button
                variant="ghost"
                className="w-full justify-start text-base h-12 text-error hover:text-error hover:bg-error-subtle"
                onClick={handleLogout}
                disabled={isLoggingOut}
              >
                {isLoggingOut ? 'Signing out...' : 'Sign out'}
              </Button>
            </div>
          </SheetContent>
        </Sheet>
      </div>
    </nav>
  );
}
