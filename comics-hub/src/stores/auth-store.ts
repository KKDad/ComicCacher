import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { AuthState, AuthTokens, LoginCredentials, RegisterData, User } from '@/types/auth';
import * as authAPI from '@/lib/api/auth';
import { setAuthToken, clearAuthToken } from '@/lib/graphql-client';
import Cookies from 'js-cookie';

interface AuthStore extends AuthState {
  _hasHydrated: boolean;
  setHasHydrated: (value: boolean) => void;
  login: (credentials: LoginCredentials) => Promise<void>;
  register: (data: RegisterData) => Promise<void>;
  logout: () => Promise<void>;
  refreshToken: () => Promise<void>;
  validateSession: () => Promise<boolean>;
  clearError: () => void;
  setUser: (user: User | null) => void;
}

// Helper to sync auth state to cookies for server-side middleware
const syncAuthCookie = (isAuthenticated: boolean, tokens: AuthTokens | null) => {
  if (isAuthenticated && tokens?.accessToken) {
    Cookies.set('auth-storage', JSON.stringify({
      state: { isAuthenticated: true, tokens }
    }), { sameSite: 'lax' });
  } else {
    Cookies.remove('auth-storage');
  }
};

const REFRESH_BUFFER_MS = 5 * 60 * 1000; // Refresh 5 minutes before expiry

export const useAuthStore = create<AuthStore>()(
  persist(
    (set, get) => ({
      user: null,
      tokens: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,
      _hasHydrated: false,
      setHasHydrated: (value: boolean) => set({ _hasHydrated: value }),

      login: async (credentials: LoginCredentials) => {
        set({ isLoading: true, error: null });
        try {
          const response = await authAPI.login(credentials);

          const tokens: AuthTokens = {
            accessToken: response.accessToken,
            refreshToken: response.refreshToken,
            expiresIn: response.expiresIn,
            expiresAt: Date.now() + response.expiresIn * 1000,
          };

          setAuthToken(response.accessToken);

          // Map partial user data to full User type
          const user: User = {
            username: response.user.username,
            displayName: response.user.displayName,
            email: '', // Not provided by login response
            roles: [], // Not provided by login response
            created: new Date().toISOString(), // Not provided by login response
          };

          set({
            user,
            tokens,
            isAuthenticated: true,
            isLoading: false,
            error: null,
          });

          // Sync to cookie for server-side middleware
          syncAuthCookie(true, tokens);
        } catch (error) {
          const message = error instanceof Error ? error.message : 'Login failed';
          set({
            isLoading: false,
            error: message,
            isAuthenticated: false,
          });
          throw error;
        }
      },

      register: async (data: RegisterData) => {
        set({ isLoading: true, error: null });
        try {
          const response = await authAPI.register(data);

          const tokens: AuthTokens = {
            accessToken: response.accessToken,
            refreshToken: response.refreshToken,
            expiresIn: response.expiresIn,
            expiresAt: Date.now() + response.expiresIn * 1000,
          };

          setAuthToken(response.accessToken);

          // Map partial user data to full User type
          const user: User = {
            username: response.user.username,
            displayName: response.user.displayName,
            email: data.email, // Use email from registration data
            roles: [], // Not provided by register response
            created: new Date().toISOString(), // Not provided by register response
          };

          set({
            user,
            tokens,
            isAuthenticated: true,
            isLoading: false,
            error: null,
          });

          // Sync to cookie for server-side middleware
          syncAuthCookie(true, tokens);
        } catch (error) {
          const message = error instanceof Error ? error.message : 'Registration failed';
          set({
            isLoading: false,
            error: message,
            isAuthenticated: false,
          });
          throw error;
        }
      },

      logout: async () => {
        try {
          await authAPI.logout();
        } catch (error) {
          console.error('Logout API error:', error);
        } finally {
          clearAuthToken();
          set({
            user: null,
            tokens: null,
            isAuthenticated: false,
            error: null,
          });

          // Remove auth cookie
          syncAuthCookie(false, null);
        }
      },

      refreshToken: async () => {
        const { tokens, user: currentUser } = get();

        if (!tokens?.refreshToken) {
          throw new Error('No refresh token available');
        }

        try {
          const response = await authAPI.refreshToken(tokens.refreshToken);

          const newTokens: AuthTokens = {
            accessToken: response.accessToken,
            refreshToken: response.refreshToken,
            expiresIn: response.expiresIn,
            expiresAt: Date.now() + response.expiresIn * 1000,
          };

          setAuthToken(response.accessToken);

          // Map partial user data to full User type, preserving existing data where possible
          const user: User = {
            username: response.user.username,
            displayName: response.user.displayName,
            email: currentUser?.email || '', // Preserve existing email
            roles: currentUser?.roles || [], // Preserve existing roles
            created: currentUser?.created || new Date().toISOString(), // Preserve existing created date
          };

          set({
            user,
            tokens: newTokens,
            isAuthenticated: true,
            error: null,
          });

          // Sync to cookie for server-side middleware
          syncAuthCookie(true, newTokens);
        } catch (error) {
          console.error('Token refresh failed:', error);
          await get().logout();
          throw error;
        }
      },

      validateSession: async () => {
        const { tokens, isAuthenticated } = get();

        if (!isAuthenticated || !tokens?.accessToken) {
          return false;
        }

        // Check if token needs refresh
        const timeUntilExpiry = tokens.expiresAt - Date.now();
        if (timeUntilExpiry <= 0) {
          // Token expired, try to refresh
          try {
            await get().refreshToken();
            return true;
          } catch {
            return false;
          }
        }

        // Proactively refresh if close to expiry
        if (timeUntilExpiry < REFRESH_BUFFER_MS) {
          get().refreshToken().catch(console.error);
        }

        // Validate token with backend
        try {
          const isValid = await authAPI.validateToken(tokens.accessToken);
          if (!isValid) {
            await get().logout();
            return false;
          }
          return true;
        } catch {
          return false;
        }
      },

      clearError: () => set({ error: null }),

      setUser: (user: User | null) => set({ user }),
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        user: state.user,
        tokens: state.tokens,
        isAuthenticated: state.isAuthenticated,
      }),
      onRehydrateStorage: () => (state) => {
        if (state) {
          // Restore token to GraphQL client immediately on hydration so
          // queries fired before AuthProvider's useEffect have the token.
          if (state.isAuthenticated && state.tokens?.accessToken) {
            setAuthToken(state.tokens.accessToken);
          }
          syncAuthCookie(state.isAuthenticated, state.tokens);
          // Must call set() via the action — direct mutation (`state._hasHydrated = true`)
          // happens after Zustand already notified subscribers, so React never sees it.
          state.setHasHydrated(true);
        }
      },
    }
  )
);

// Auto-refresh token interval
let refreshInterval: NodeJS.Timeout | null = null;

export function startTokenRefresh() {
  if (refreshInterval) return;

  refreshInterval = setInterval(() => {
    const { tokens, isAuthenticated, refreshToken } = useAuthStore.getState();

    if (!isAuthenticated || !tokens) return;

    const timeUntilExpiry = tokens.expiresAt - Date.now();

    // Refresh if within buffer window
    if (timeUntilExpiry < REFRESH_BUFFER_MS && timeUntilExpiry > 0) {
      refreshToken().catch((error) => {
        console.error('Auto-refresh failed:', error);
      });
    }
  }, 60 * 1000); // Check every minute
}

export function stopTokenRefresh() {
  if (refreshInterval) {
    clearInterval(refreshInterval);
    refreshInterval = null;
  }
}
