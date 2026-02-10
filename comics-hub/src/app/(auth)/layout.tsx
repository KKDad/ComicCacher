'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/hooks/use-auth';

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();
  const { isAuthenticated, isLoading, _hasHydrated } = useAuth();

  useEffect(() => {
    if (_hasHydrated && !isLoading && isAuthenticated) {
      router.replace('/');
    }
  }, [isAuthenticated, isLoading, _hasHydrated, router]);

  // Wait for hydration before making auth decisions
  if (!_hasHydrated) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-canvas">
        <div className="text-ink-subtle">Loading...</div>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-canvas">
        <div className="text-ink-subtle">Loading...</div>
      </div>
    );
  }

  if (isAuthenticated) {
    return null;
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-canvas p-4">
      <div className="w-full max-w-[420px]">
        {children}
      </div>
    </div>
  );
}
