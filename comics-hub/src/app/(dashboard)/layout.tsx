'use client';

import { useRequireAuth } from '@/hooks/use-auth';
import { useResponsiveNav } from '@/hooks/use-responsive-nav';
import { Header } from '@/components/layout/header';
import { Sidebar } from '@/components/layout/sidebar';
import { NavRail } from '@/components/layout/nav-rail';
import { MobileNav } from '@/components/layout/mobile-nav';

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { isLoading, _hasHydrated } = useRequireAuth();
  const { layout } = useResponsiveNav();

  if (!_hasHydrated || isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-canvas">
        <div className="text-ink-subtle">Loading...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-canvas">
      <Header showMenuButton={layout === 'mobile'} />

      {/* Desktop Sidebar */}
      {layout === 'desktop' && <Sidebar />}

      {/* Tablet Nav Rail */}
      {layout === 'tablet' && <NavRail />}

      {/* Main content */}
      <main
        className={`
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
