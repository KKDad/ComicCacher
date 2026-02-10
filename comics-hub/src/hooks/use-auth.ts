import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/stores/auth-store';

export function useAuth() {
  return useAuthStore();
}

export function useRequireAuth() {
  const router = useRouter();
  const { isAuthenticated, isLoading, validateSession, _hasHydrated } = useAuthStore();

  useEffect(() => {
    const checkAuth = async () => {
      // Wait for hydration before checking
      if (_hasHydrated && !isLoading) {
        const isValid = await validateSession();
        if (!isValid) {
          router.replace('/login');
        }
      }
    };

    checkAuth();
  }, [isAuthenticated, isLoading, _hasHydrated, router, validateSession]);

  return { isAuthenticated, isLoading, _hasHydrated };
}
