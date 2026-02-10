'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  LayoutDashboard,
  BookOpen,
  BarChart3,
  Menu,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet';
import { Separator } from '@/components/ui/separator';
import { useState } from 'react';
import { useAuth } from '@/hooks/use-auth';

const bottomNavItems = [
  { href: '/', label: 'Dashboard', icon: LayoutDashboard },
  { href: '/comics', label: 'Comics', icon: BookOpen },
  { href: '/metrics', label: 'Metrics', icon: BarChart3 },
];

const menuItems = [
  { href: '/retrieval-status', label: 'Retrieval Status' },
  { href: '/api', label: 'API' },
  { href: '/preferences', label: 'Preferences' },
];

export function MobileNav() {
  const pathname = usePathname();
  const { logout } = useAuth();
  const [isOpen, setIsOpen] = useState(false);

  const handleLogout = async () => {
    setIsOpen(false);
    await logout();
  };

  return (
    <>
      {/* Bottom Navigation Bar */}
      <nav className="fixed bottom-0 left-0 right-0 h-[var(--mobile-nav-height)] bg-surface border-t border-border z-fixed">
        <div className="flex items-center justify-around h-full">
          {bottomNavItems.map((item) => {
            const Icon = item.icon;
            const isActive = pathname === item.href;

            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  'flex flex-col items-center justify-center flex-1 h-full gap-1 transition-colors',
                  isActive
                    ? 'text-primary'
                    : 'text-ink-muted hover:text-ink'
                )}
              >
                <Icon className="h-5 w-5" />
                <span className="text-xs font-medium">{item.label}</span>
              </Link>
            );
          })}

          {/* Hamburger Menu */}
          <Sheet open={isOpen} onOpenChange={setIsOpen}>
            <SheetTrigger asChild>
              <button
                className="flex flex-col items-center justify-center flex-1 h-full gap-1 text-ink-muted hover:text-ink transition-colors"
              >
                <Menu className="h-5 w-5" />
                <span className="text-xs font-medium">More</span>
              </button>
            </SheetTrigger>
            <SheetContent side="bottom" className="rounded-t-xl">
              <SheetHeader>
                <SheetTitle>Menu</SheetTitle>
              </SheetHeader>
              <div className="mt-6 space-y-1">
                {menuItems.map((item) => (
                  <Link
                    key={item.href}
                    href={item.href}
                    onClick={() => setIsOpen(false)}
                  >
                    <Button
                      variant="ghost"
                      className="w-full justify-start text-base h-12"
                    >
                      {item.label}
                    </Button>
                  </Link>
                ))}
                <Separator className="my-2" />
                <Button
                  variant="ghost"
                  className="w-full justify-start text-base h-12 text-error hover:text-error hover:bg-error-subtle"
                  onClick={handleLogout}
                >
                  Logout
                </Button>
              </div>
            </SheetContent>
          </Sheet>
        </div>
      </nav>
    </>
  );
}
