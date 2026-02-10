import { describe, it, expect, beforeEach, vi } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { useAuth, useRequireAuth } from './use-auth';
import { useAuthStore } from '@/stores/auth-store';
import { useRouter } from 'next/navigation';

vi.mock('@/stores/auth-store');
vi.mock('next/navigation');

describe('useAuth', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should return auth store state and methods', () => {
    const mockAuthState = {
      user: {
        id: '1',
        username: 'testuser',
        email: 'test@example.com',
        displayName: 'Test User',
        role: 'USER' as const,
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
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      refreshToken: vi.fn(),
      validateSession: vi.fn(),
      clearError: vi.fn(),
      setUser: vi.fn(),
    };

    vi.mocked(useAuthStore).mockReturnValue(mockAuthState);

    const { result } = renderHook(() => useAuth());

    expect(result.current.user).toEqual(mockAuthState.user);
    expect(result.current.isAuthenticated).toBe(true);
    expect(result.current.tokens).toEqual(mockAuthState.tokens);
  });

  it('should return null user when not authenticated', () => {
    const mockAuthState = {
      user: null,
      tokens: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      refreshToken: vi.fn(),
      validateSession: vi.fn(),
      clearError: vi.fn(),
      setUser: vi.fn(),
    };

    vi.mocked(useAuthStore).mockReturnValue(mockAuthState);

    const { result } = renderHook(() => useAuth());

    expect(result.current.user).toBeNull();
    expect(result.current.isAuthenticated).toBe(false);
  });
});

describe('useRequireAuth', () => {
  const mockRouter = {
    push: vi.fn(),
    replace: vi.fn(),
    prefetch: vi.fn(),
    back: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useRouter).mockReturnValue(mockRouter as any);
  });

  it('should not redirect when authenticated', async () => {
    const mockAuthState = {
      user: {
        id: '1',
        username: 'testuser',
        email: 'test@example.com',
        displayName: 'Test User',
        role: 'USER' as const,
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
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      refreshToken: vi.fn(),
      validateSession: vi.fn().mockResolvedValue(true),
      clearError: vi.fn(),
      setUser: vi.fn(),
    };

    vi.mocked(useAuthStore).mockReturnValue(mockAuthState);

    const { result } = renderHook(() => useRequireAuth());

    await waitFor(() => {
      expect(mockAuthState.validateSession).toHaveBeenCalled();
    });

    expect(mockRouter.replace).not.toHaveBeenCalled();
    expect(result.current.isAuthenticated).toBe(true);
  });

  it('should redirect to login when not authenticated', async () => {
    const mockAuthState = {
      user: null,
      tokens: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      refreshToken: vi.fn(),
      validateSession: vi.fn().mockResolvedValue(false),
      clearError: vi.fn(),
      setUser: vi.fn(),
    };

    vi.mocked(useAuthStore).mockReturnValue(mockAuthState);

    const { result } = renderHook(() => useRequireAuth());

    await waitFor(() => {
      expect(mockRouter.replace).toHaveBeenCalledWith('/login');
    });

    expect(result.current.isAuthenticated).toBe(false);
  });

  it('should not redirect while loading', () => {
    const mockAuthState = {
      user: null,
      tokens: null,
      isAuthenticated: false,
      isLoading: true, // Still loading
      error: null,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      refreshToken: vi.fn(),
      validateSession: vi.fn(),
      clearError: vi.fn(),
      setUser: vi.fn(),
    };

    vi.mocked(useAuthStore).mockReturnValue(mockAuthState);

    renderHook(() => useRequireAuth());

    expect(mockRouter.replace).not.toHaveBeenCalled();
    expect(mockAuthState.validateSession).not.toHaveBeenCalled();
  });

  it('should redirect when session validation fails', async () => {
    const mockAuthState = {
      user: {
        id: '1',
        username: 'testuser',
        email: 'test@example.com',
        displayName: 'Test User',
        role: 'USER' as const,
        createdAt: '2024-01-01T00:00:00Z',
      },
      tokens: {
        accessToken: 'expired-token',
        refreshToken: 'refresh',
        expiresIn: 3600,
        expiresAt: Date.now() - 1000, // Expired
      },
      isAuthenticated: true,
      isLoading: false,
      error: null,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      refreshToken: vi.fn(),
      validateSession: vi.fn().mockResolvedValue(false), // Validation fails
      clearError: vi.fn(),
      setUser: vi.fn(),
    };

    vi.mocked(useAuthStore).mockReturnValue(mockAuthState);

    renderHook(() => useRequireAuth());

    await waitFor(() => {
      expect(mockRouter.replace).toHaveBeenCalledWith('/login');
    });
  });
});
