'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils';
import { baseNavItems, isNavActive, navIconStroke } from './nav-items';

// Four direct tabs. Sign out and, for operators, the operations pages live in the
// account menu in the header, which is on screen at every size.
export function MobileNav() {
  const pathname = usePathname();

  return (
    <nav
      aria-label="Main"
      className="fixed bottom-0 left-0 right-0 bg-chrome border-t border-border z-fixed pb-[env(safe-area-inset-bottom)]"
    >
      <div className="flex items-stretch justify-around h-[var(--mobile-nav-height)]">
        {baseNavItems.map((item) => {
          const Icon = item.icon;
          const active = isNavActive(pathname, item.href);

          return (
            <Link
              key={item.href}
              href={item.href}
              aria-current={active ? 'page' : undefined}
              className={cn(
                'flex flex-1 flex-col items-center justify-center gap-0.5 min-h-11 transition-colors',
                active ? 'text-primary' : 'text-ink-subtle hover:text-ink',
              )}
            >
              <span
                className={cn(
                  'flex h-7 w-14 items-center justify-center rounded-full transition-colors',
                  active && 'bg-primary-subtle',
                )}
              >
                <Icon className="size-[22px]" strokeWidth={navIconStroke(active)} aria-hidden="true" />
              </span>
              <span className={cn('text-xs', active ? 'font-bold' : 'font-medium')}>{item.shortLabel}</span>
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
