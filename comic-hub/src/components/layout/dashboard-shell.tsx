'use client';

import { useResponsiveNav } from '@/hooks/use-responsive-nav';
import { Header } from '@/components/layout/header';
import { Sidebar } from '@/components/layout/sidebar';
import { NavRail } from '@/components/layout/nav-rail';
import { MobileNav } from '@/components/layout/mobile-nav';

interface DashboardShellProps {
  children: React.ReactNode;
}

export function DashboardShell({ children }: DashboardShellProps) {
  const { layout } = useResponsiveNav();

  return (
    <div className="min-h-screen bg-canvas">
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:fixed focus:top-2 focus:left-2 focus:z-toast focus:rounded-md focus:bg-surface focus:px-4 focus:py-2 focus:shadow-lg focus:text-primary"
      >
        Skip to content
      </a>
      <Header />

      {/* Desktop Sidebar */}
      {layout === 'desktop' && <Sidebar />}

      {/* Tablet Nav Rail */}
      {layout === 'tablet' && <NavRail />}

      {/* Main content */}
      <main
        id="main-content"
        tabIndex={-1}
        className={`outline-none
          relative z-0
          pt-[var(--header-height)]
          ${layout === 'desktop' ? 'pl-[var(--sidebar-width)]' : ''}
          ${layout === 'tablet' ? 'pl-[var(--sidebar-collapsed)]' : ''}
          ${layout === 'mobile' ? 'pb-[var(--mobile-nav-height)]' : ''}
        `}
      >
        <div className="container mx-auto p-4 lg:p-6 max-w-[var(--content-max-width)]">
          {children}
        </div>
      </main>

      {/* Mobile Bottom Nav */}
      {layout === 'mobile' && <MobileNav />}
    </div>
  );
}
