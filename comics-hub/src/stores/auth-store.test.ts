import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { useAuthStore } from './auth-store';
import * as authAPI from '@/lib/api/auth';
import * as graphqlClient from '@/lib/graphql-client';

vi.mock('@/lib/api/auth');
vi.mock('@/lib/graphql-client');

describe('Auth Store', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    // Reset store to initial state
    useAuthStore.setState({
      user: null,
      tokens: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,
    });
  });

  describe('login', () => {
    it('should successfully login and update state', async () => {
      const mockAuthResponse = {
        user: {
          username: 'testuser',
          displayName: 'Test User',
        },
        accessToken: 'access-token-123',
        refreshToken: 'refresh-token-456',
        expiresIn: 3600,
      };

      vi.mocked(authAPI.login).mockResolvedValue(mockAuthResponse);

      const { result } = renderHook(() => useAuthStore());

      await act(async () => {
        await result.current.login({
          username: 'testuser',
          password: 'password123',
        });
      });

      expect(result.current.isAuthenticated).toBe(true);
      expect(result.current.user?.username).toBe('testuser');
      expect(result.current.user?.displayName).toBe('Test User');
      expect(result.current.tokens?.accessToken).toBe('access-token-123');
      expect(result.current.error).toBeNull();
      expect(graphqlClient.setAuthToken).toHaveBeenCalledWith('access-token-123');
    });

    it('should set error on login failure', async () => {
      vi.mocked(authAPI.login).mockRejectedValue(
        new Error('Invalid credentials')
      );

      const { result } = renderHook(() => useAuthStore());

      await act(async () => {
        try {
          await result.current.login({
            username: 'wronguser',
            password: 'wrongpass',
          });
        } catch (error) {
          // Expected to throw
        }
      });

      expect(result.current.isAuthenticated).toBe(false);
      expect(result.current.user).toBeNull();
      expect(result.current.error).toBe('Invalid credentials');
    });

    it('should set tokens with correct expiry time', async () => {
      const now = Date.now();
      const mockAuthResponse = {
        user: {
          username: 'testuser',
          displayName: 'Test User',
        },
        accessToken: 'token',
        refreshToken: 'refresh',
        expiresIn: 3600,
      };

      vi.mocked(authAPI.login).mockResolvedValue(mockAuthResponse);

      const { result } = renderHook(() => useAuthStore());

      await act(async () => {
        await result.current.login({
          username: 'testuser',
          password: 'password123',
        });
      });

      expect(result.current.tokens?.expiresAt).toBeGreaterThanOrEqual(now + 3600000);
    });
  });

  describe('register', () => {
    it('should successfully register and update state', async () => {
      const mockAuthResponse = {
        user: {
          username: 'newuser',
          displayName: 'New User',
        },
        accessToken: 'access-token-789',
        refreshToken: 'refresh-token-012',
        expiresIn: 3600,
      };

      vi.mocked(authAPI.register).mockResolvedValue(mockAuthResponse);

      const { result } = renderHook(() => useAuthStore());

      await act(async () => {
        await result.current.register({
          username: 'newuser',
          email: 'new@example.com',
          displayName: 'New User',
          password: 'SecurePass123',
        });
      });

      expect(result.current.isAuthenticated).toBe(true);
      expect(result.current.user?.username).toBe('newuser');
      expect(graphqlClient.setAuthToken).toHaveBeenCalledWith('access-token-789');
    });

    it('should set error on registration failure', async () => {
      vi.mocked(authAPI.register).mockRejectedValue(
        new Error('Username already exists')
      );

      const { result } = renderHook(() => useAuthStore());

      await act(async () => {
        try {
          await result.current.register({
            username: 'existinguser',
            email: 'test@example.com',
            displayName: 'Test',
            password: 'pass123',
          });
        } catch (error) {
          // Expected to throw
        }
      });

      expect(result.current.isAuthenticated).toBe(false);
      expect(result.current.error).toBe('Username already exists');
    });
  });

  describe('logout', () => {
    it('should clear auth state and call API', async () => {
      // Set up authenticated state first
      useAuthStore.setState({
        user: {
          id: '1',
          username: 'testuser',
          email: 'test@example.com',
          displayName: 'Test User',
          role: 'USER',
          createdAt: '2024-01-01T00:00:00Z',
        },
        tokens: {
          accessToken: 'token',
          refreshToken: 'refresh',
          expiresIn: 3600,
          expiresAt: Date.now() + 3600000,
        },
        isAuthenticated: true,
        isLoading: false,
        error: null,
      });

      vi.mocked(authAPI.logout).mockResolvedValue();

      const { result } = renderHook(() => useAuthStore());

      await act(async () => {
        await result.current.logout();
      });

      expect(authAPI.logout).toHaveBeenCalled();
      expect(result.current.isAuthenticated).toBe(false);
      expect(result.current.user).toBeNull();
      expect(result.current.tokens).toBeNull();
      expect(graphqlClient.clearAuthToken).toHaveBeenCalled();
    });

    it('should clear state even if API call fails', async () => {
      useAuthStore.setState({
        user: {
          id: '1',
          username: 'testuser',
          email: 'test@example.com',
          displayName: 'Test User',
          role: 'USER',
          createdAt: '2024-01-01T00:00:00Z',
        },
        tokens: {
          accessToken: 'token',
          refreshToken: 'refresh',
          expiresIn: 3600,
          expiresAt: Date.now() + 3600000,
        },
        isAuthenticated: true,
        isLoading: false,
        error: null,
      });

      vi.mocked(authAPI.logout).mockRejectedValue(new Error('Network error'));

      const { result } = renderHook(() => useAuthStore());

      await act(async () => {
        await result.current.logout();
      });

      expect(result.current.isAuthenticated).toBe(false);
      expect(result.current.user).toBeNull();
      expect(graphqlClient.clearAuthToken).toHaveBeenCalled();
    });
  });

  describe('refreshToken', () => {
    it('should successfully refresh tokens', async () => {
      const mockRefreshResponse = {
        user: {
          username: 'testuser',
          displayName: 'Test User',
        },
        accessToken: 'new-access-token',
        refreshToken: 'new-refresh-token',
        expiresIn: 3600,
      };

      useAuthStore.setState({
        user: {
          id: '1',
          username: 'testuser',
          email: 'test@example.com',
          displayName: 'Test User',
          role: 'USER',
          createdAt: '2024-01-01T00:00:00Z',
        },
        tokens: {
          accessToken: 'old-token',
          refreshToken: 'old-refresh',
          expiresIn: 3600,
          expiresAt: Date.now() + 60000,
        },
        isAuthenticated: true,
        isLoading: false,
        error: null,
      });

      vi.mocked(authAPI.refreshToken).mockResolvedValue(mockRefreshResponse);

      const { result } = renderHook(() => useAuthStore());

      await act(async () => {
        await result.current.refreshToken();
      });

      expect(authAPI.refreshToken).toHaveBeenCalledWith('old-refresh');
      expect(result.current.tokens?.accessToken).toBe('new-access-token');
      expect(graphqlClient.setAuthToken).toHaveBeenCalledWith('new-access-token');
    });

    it('should logout if refresh fails', async () => {
      useAuthStore.setState({
        user: {
          id: '1',
          username: 'testuser',
          email: 'test@example.com',
          displayName: 'Test User',
          role: 'USER',
          createdAt: '2024-01-01T00:00:00Z',
        },
        tokens: {
          accessToken: 'old-token',
          refreshToken: 'old-refresh',
          expiresIn: 3600,
          expiresAt: Date.now() + 60000,
        },
        isAuthenticated: true,
        isLoading: false,
        error: null,
      });

      vi.mocked(authAPI.refreshToken).mockRejectedValue(
        new Error('Invalid refresh token')
      );
      vi.mocked(authAPI.logout).mockResolvedValue();

      const { result } = renderHook(() => useAuthStore());

      await act(async () => {
        try {
          await result.current.refreshToken();
        } catch (error) {
          // Expected to throw
        }
      });

      await waitFor(() => {
        expect(result.current.isAuthenticated).toBe(false);
      });
    });

    it('should throw error if no refresh token available', async () => {
      useAuthStore.setState({
        user: null,
        tokens: null,
        isAuthenticated: false,
        isLoading: false,
        error: null,
      });

      const { result } = renderHook(() => useAuthStore());

      await expect(
        act(async () => {
          await result.current.refreshToken();
        })
      ).rejects.toThrow('No refresh token available');
    });
  });

  describe('validateSession', () => {
    it('should return true for valid non-expired token', async () => {
      useAuthStore.setState({
        user: {
          id: '1',
          username: 'testuser',
          email: 'test@example.com',
          displayName: 'Test User',
          role: 'USER',
          createdAt: '2024-01-01T00:00:00Z',
        },
        tokens: {
          accessToken: 'valid-token',
          refreshToken: 'refresh',
          expiresIn: 3600,
          expiresAt: Date.now() + 3600000, // 1 hour from now
        },
        isAuthenticated: true,
        isLoading: false,
        error: null,
      });

      vi.mocked(authAPI.validateToken).mockResolvedValue(true);

      const { result } = renderHook(() => useAuthStore());

      await act(async () => {
        const isValid = await result.current.validateSession();
        expect(isValid).toBe(true);
      });
    });

    it('should refresh token if close to expiry', async () => {
      const mockRefreshResponse = {
        user: {
          username: 'testuser',
          displayName: 'Test User',
        },
        accessToken: 'new-token',
        refreshToken: 'new-refresh',
        expiresIn: 3600,
      };

      useAuthStore.setState({
        user: {
          id: '1',
          username: 'testuser',
          email: 'test@example.com',
          displayName: 'Test User',
          role: 'USER',
          createdAt: '2024-01-01T00:00:00Z',
        },
        tokens: {
          accessToken: 'old-token',
          refreshToken: 'old-refresh',
          expiresIn: 3600,
          expiresAt: Date.now() + 120000, // 2 minutes from now (within buffer)
        },
        isAuthenticated: true,
        isLoading: false,
        error: null,
      });

      vi.mocked(authAPI.validateToken).mockResolvedValue(true);
      vi.mocked(authAPI.refreshToken).mockResolvedValue(mockRefreshResponse);

      const { result } = renderHook(() => useAuthStore());

      await act(async () => {
        await result.current.validateSession();
      });

      // Should trigger refresh in background
      await waitFor(() => {
        expect(authAPI.refreshToken).toHaveBeenCalled();
      });
    });

    it('should return false for unauthenticated user', async () => {
      useAuthStore.setState({
        user: null,
        tokens: null,
        isAuthenticated: false,
        isLoading: false,
        error: null,
      });

      const { result } = renderHook(() => useAuthStore());

      const isValid = await result.current.validateSession();
      expect(isValid).toBe(false);
    });

    it('should attempt refresh if token is expired', async () => {
      const mockRefreshResponse = {
        user: {
          username: 'testuser',
          displayName: 'Test User',
        },
        accessToken: 'new-token',
        refreshToken: 'new-refresh',
        expiresIn: 3600,
      };

      useAuthStore.setState({
        user: {
          id: '1',
          username: 'testuser',
          email: 'test@example.com',
          displayName: 'Test User',
          role: 'USER',
          createdAt: '2024-01-01T00:00:00Z',
        },
        tokens: {
          accessToken: 'expired-token',
          refreshToken: 'refresh',
          expiresIn: 3600,
          expiresAt: Date.now() - 1000, // Already expired
        },
        isAuthenticated: true,
        isLoading: false,
        error: null,
      });

      vi.mocked(authAPI.refreshToken).mockResolvedValue(mockRefreshResponse);

      const { result } = renderHook(() => useAuthStore());

      await act(async () => {
        const isValid = await result.current.validateSession();
        expect(isValid).toBe(true);
      });

      expect(authAPI.refreshToken).toHaveBeenCalled();
    });
  });

  describe('clearError', () => {
    it('should clear error state', () => {
      useAuthStore.setState({
        user: null,
        tokens: null,
        isAuthenticated: false,
        isLoading: false,
        error: 'Some error',
      });

      const { result } = renderHook(() => useAuthStore());

      act(() => {
        result.current.clearError();
      });

      expect(result.current.error).toBeNull();
    });
  });

  describe('setUser', () => {
    it('should update user state', () => {
      const newUser = {
        id: '1',
        username: 'testuser',
        email: 'test@example.com',
        displayName: 'Test User',
        role: 'USER' as const,
        createdAt: '2024-01-01T00:00:00Z',
      };

      const { result } = renderHook(() => useAuthStore());

      act(() => {
        result.current.setUser(newUser);
      });

      expect(result.current.user).toEqual(newUser);
    });

    it('should clear user when set to null', () => {
      useAuthStore.setState({
        user: {
          id: '1',
          username: 'testuser',
          email: 'test@example.com',
          displayName: 'Test User',
          role: 'USER',
          createdAt: '2024-01-01T00:00:00Z',
        },
        tokens: null,
        isAuthenticated: false,
        isLoading: false,
        error: null,
      });

      const { result } = renderHook(() => useAuthStore());

      act(() => {
        result.current.setUser(null);
      });

      expect(result.current.user).toBeNull();
    });
  });
});
