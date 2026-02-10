'use client';

import { useEffect, type ReactNode } from 'react';
import { useAuthStore, startTokenRefresh, stopTokenRefresh } from '@/stores/auth-store';
import { setAuthToken } from '@/lib/graphql-client';

export function AuthProvider({ children }: { children: ReactNode }) {
  const { tokens, isAuthenticated, validateSession } = useAuthStore();

  useEffect(() => {
    // Initialize auth state on mount
    const initAuth = async () => {
      if (isAuthenticated && tokens?.accessToken) {
        // Restore token to GraphQL client
        setAuthToken(tokens.accessToken);

        // Validate the session
        await validateSession();
      }
    };

    initAuth();

    // Start automatic token refresh
    startTokenRefresh();

    return () => {
      stopTokenRefresh();
    };
  }, []);

  return <>{children}</>;
}
