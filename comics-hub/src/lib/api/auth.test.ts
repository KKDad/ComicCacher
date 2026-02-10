import { describe, it, expect, beforeEach, vi } from 'vitest';
import * as authAPI from './auth';
import type { LoginCredentials, RegisterData } from '@/types/auth';

describe('Auth API', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    global.fetch = vi.fn();
  });

  describe('login', () => {
    it('should successfully login with valid credentials', async () => {
      const mockResponse = {
        user: {
          id: '1',
          username: 'testuser',
          email: 'test@example.com',
          displayName: 'Test User',
          role: 'USER' as const,
          createdAt: '2024-01-01T00:00:00Z',
        },
        accessToken: 'access-token-123',
        refreshToken: 'refresh-token-456',
        expiresIn: 3600,
      };

      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => mockResponse,
      });

      const credentials: LoginCredentials = {
        username: 'testuser',
        password: 'password123',
      };

      const result = await authAPI.login(credentials);

      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/auth/login'),
        expect.objectContaining({
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            username: credentials.username,
            password: credentials.password,
          }),
        })
      );

      expect(result).toEqual(mockResponse);
    });

    it('should throw AuthAPIError on invalid credentials', async () => {
      (global.fetch as any).mockResolvedValueOnce({
        ok: false,
        status: 401,
        statusText: 'Unauthorized',
        json: async () => ({
          message: 'Invalid username or password',
          code: 'INVALID_CREDENTIALS',
        }),
      });

      const credentials: LoginCredentials = {
        username: 'wronguser',
        password: 'wrongpass',
      };

      await expect(authAPI.login(credentials)).rejects.toThrow(
        'Invalid username or password'
      );
    });

    it('should handle network errors', async () => {
      (global.fetch as any).mockRejectedValueOnce(new Error('Network error'));

      const credentials: LoginCredentials = {
        username: 'testuser',
        password: 'password123',
      };

      await expect(authAPI.login(credentials)).rejects.toThrow('Network error');
    });
  });

  describe('register', () => {
    it('should successfully register a new user', async () => {
      const mockResponse = {
        user: {
          id: '2',
          username: 'newuser',
          email: 'new@example.com',
          displayName: 'New User',
          role: 'USER' as const,
          createdAt: '2024-01-01T00:00:00Z',
        },
        accessToken: 'access-token-789',
        refreshToken: 'refresh-token-012',
        expiresIn: 3600,
      };

      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => mockResponse,
      });

      const data: RegisterData = {
        username: 'newuser',
        email: 'new@example.com',
        displayName: 'New User',
        password: 'SecurePass123',
      };

      const result = await authAPI.register(data);

      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/auth/register'),
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify(data),
        })
      );

      expect(result).toEqual(mockResponse);
    });

    it('should throw error when username is taken', async () => {
      (global.fetch as any).mockResolvedValueOnce({
        ok: false,
        status: 409,
        statusText: 'Conflict',
        json: async () => ({
          message: 'Username already exists',
          code: 'USERNAME_TAKEN',
        }),
      });

      const data: RegisterData = {
        username: 'existinguser',
        email: 'new@example.com',
        displayName: 'New User',
        password: 'SecurePass123',
      };

      await expect(authAPI.register(data)).rejects.toThrow(
        'Username already exists'
      );
    });
  });

  describe('refreshToken', () => {
    it('should successfully refresh access token', async () => {
      const mockResponse = {
        user: {
          id: '1',
          username: 'testuser',
          email: 'test@example.com',
          displayName: 'Test User',
          role: 'USER' as const,
          createdAt: '2024-01-01T00:00:00Z',
        },
        accessToken: 'new-access-token',
        refreshToken: 'new-refresh-token',
        expiresIn: 3600,
      };

      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: async () => mockResponse,
      });

      const result = await authAPI.refreshToken('old-refresh-token');

      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/auth/refresh'),
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify({ refreshToken: 'old-refresh-token' }),
        })
      );

      expect(result.accessToken).toBe('new-access-token');
    });

    it('should throw error on invalid refresh token', async () => {
      (global.fetch as any).mockResolvedValueOnce({
        ok: false,
        status: 401,
        statusText: 'Unauthorized',
        json: async () => ({
          message: 'Invalid refresh token',
        }),
      });

      await expect(authAPI.refreshToken('invalid-token')).rejects.toThrow(
        'Invalid refresh token'
      );
    });
  });

  describe('logout', () => {
    it('should successfully logout', async () => {
      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
      });

      await expect(authAPI.logout('access-token')).resolves.toBeUndefined();

      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/auth/logout'),
        expect.objectContaining({
          method: 'POST',
          headers: {
            Authorization: 'Bearer access-token',
          },
        })
      );
    });

    it('should throw error on logout failure', async () => {
      (global.fetch as any).mockResolvedValueOnce({
        ok: false,
        status: 500,
      });

      await expect(authAPI.logout('access-token')).rejects.toThrow(
        'Logout failed'
      );
    });
  });

  describe('validateToken', () => {
    it('should return true for valid token', async () => {
      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
      });

      const result = await authAPI.validateToken('valid-token');

      expect(result).toBe(true);
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/auth/validate'),
        expect.objectContaining({
          method: 'GET',
          headers: {
            Authorization: 'Bearer valid-token',
          },
        })
      );
    });

    it('should return false for invalid token', async () => {
      (global.fetch as any).mockResolvedValueOnce({
        ok: false,
        status: 401,
      });

      const result = await authAPI.validateToken('invalid-token');

      expect(result).toBe(false);
    });

    it('should return false on network error', async () => {
      (global.fetch as any).mockRejectedValueOnce(new Error('Network error'));

      const result = await authAPI.validateToken('token');

      expect(result).toBe(false);
    });
  });

  describe('requestPasswordReset', () => {
    it('should successfully request password reset', async () => {
      (global.fetch as any).mockResolvedValueOnce({
        ok: true,
      });

      await expect(
        authAPI.requestPasswordReset('test@example.com')
      ).resolves.toBeUndefined();

      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/auth/forgot-password'),
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify({ email: 'test@example.com' }),
        })
      );
    });

    it('should throw error for non-existent email', async () => {
      (global.fetch as any).mockResolvedValueOnce({
        ok: false,
        status: 404,
        json: async () => ({
          message: 'Email not found',
        }),
      });

      await expect(
        authAPI.requestPasswordReset('nonexistent@example.com')
      ).rejects.toThrow('Email not found');
    });
  });
});
