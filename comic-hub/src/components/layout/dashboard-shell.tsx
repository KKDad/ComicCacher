import { Header } from '@/components/layout/header';
import { Sidebar } from '@/components/layout/sidebar';
import { NavRail } from '@/components/layout/nav-rail';
import { MobileNav } from '@/components/layout/mobile-nav';

interface DashboardShellProps {
  children: React.ReactNode;
}

/**
 * All three navigations are rendered and CSS picks one per breakpoint, so the
 * server-rendered page already has the right layout — no post-hydration jump.
 */
export function DashboardShell({ children }: DashboardShellProps) {
  return (
    <div className="min-h-dvh bg-canvas">
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:fixed focus:top-2 focus:left-2 focus:z-toast focus:rounded-md focus:bg-surface focus:px-4 focus:py-2 focus:shadow-lg focus:text-primary"
      >
        Skip to content
      </a>
      <Header />

      <div className="hidden lg:block">
        <Sidebar />
      </div>
      <div className="hidden md:block lg:hidden">
        <NavRail />
      </div>

      <main
        id="main-content"
        tabIndex={-1}
        className="relative z-base outline-none pt-[var(--header-height)] pb-[calc(var(--mobile-nav-height)+env(safe-area-inset-bottom))] md:pb-0 md:pl-[var(--sidebar-collapsed)] lg:pl-[var(--sidebar-width)]"
      >
        <div className="container mx-auto p-4 lg:p-6 max-w-[var(--content-max-width)]">
          {children}
        </div>
      </main>

      <div className="md:hidden">
        <MobileNav />
      </div>
    </div>
  );
}
