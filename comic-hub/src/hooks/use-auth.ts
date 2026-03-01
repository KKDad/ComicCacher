'use client';

import { useCallback, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useQueryClient } from '@tanstack/react-query';

export function useLogout() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  const logout = useCallback(async () => {
    setIsLoggingOut(true);
    try {
      await fetch('/api/logout', { method: 'POST' });
      queryClient.clear();
      router.push('/login');
      router.refresh();
    } finally {
      setIsLoggingOut(false);
    }
  }, [router, queryClient]);

  return { logout, isLoggingOut };
}
