import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/stores/auth-store';

export function useAuth() {
  return useAuthStore();
}

export function useRequireAuth() {
  const router = useRouter();
  const { isAuthenticated, isLoading, validateSession } = useAuthStore();

  useEffect(() => {
    const checkAuth = async () => {
      if (!isLoading) {
        const isValid = await validateSession();
        if (!isValid) {
          router.replace('/login');
        }
      }
    };

    checkAuth();
  }, [isAuthenticated, isLoading, router, validateSession]);

  return { isAuthenticated, isLoading };
}
