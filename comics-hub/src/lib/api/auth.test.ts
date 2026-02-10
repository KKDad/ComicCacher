import { describe, it, expect, beforeEach, vi } from 'vitest';
import * as authAPI from './auth';
import { graphqlClient, setAuthToken, clearAuthToken } from '../graphql-client';
import type { LoginCredentials, RegisterData } from '@/types/auth';

// Mock the graphql-client module
vi.mock('../graphql-client', () => ({
  graphqlClient: {
    request: vi.fn(),
    setHeader: vi.fn(),
    requestConfig: {
      headers: {},
    },
  },
  setAuthToken: vi.fn(),
  clearAuthToken: vi.fn(),
}));

describe('Auth API', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('login', () => {
    it('should successfully login with valid credentials', async () => {
      const mockGraphQLResponse = {
        login: {
          token: 'access-token-123',
          refreshToken: 'refresh-token-456',
          username: 'testuser',
          displayName: 'Test User',
        },
      };

      vi.mocked(graphqlClient.request).mockResolvedValueOnce(mockGraphQLResponse);

      const credentials: LoginCredentials = {
        username: 'testuser',
        password: 'password123',
      };

      const result = await authAPI.login(credentials);

      expect(graphqlClient.request).toHaveBeenCalledWith(
        expect.any(String), // The LOGIN_MUTATION
        {
          input: {
            username: credentials.username,
            password: credentials.password,
          },
        }
      );

      expect(result).toEqual({
        user: {
          username: 'testuser',
          displayName: 'Test User',
        },
        accessToken: 'access-token-123',
        refreshToken: 'refresh-token-456',
        expiresIn: expect.any(Number),
      });

      expect(setAuthToken).toHaveBeenCalledWith('access-token-123');
    });

    it('should throw AuthAPIError on invalid credentials', async () => {
      const mockError = {
        response: {
          errors: [
            {
              message: 'Invalid username or password',
              extensions: {
                code: 'INVALID_CREDENTIALS',
              },
            },
          ],
        },
      };

      vi.mocked(graphqlClient.request).mockRejectedValueOnce(mockError);

      const credentials: LoginCredentials = {
        username: 'wronguser',
        password: 'wrongpass',
      };

      await expect(authAPI.login(credentials)).rejects.toThrow(
        'Invalid username or password'
      );
    });

    it('should handle network errors', async () => {
      vi.mocked(graphqlClient.request).mockRejectedValueOnce(
        new Error('Network error')
      );

      const credentials: LoginCredentials = {
        username: 'testuser',
        password: 'password123',
      };

      await expect(authAPI.login(credentials)).rejects.toThrow(
        'Network error'
      );
    });
  });

  describe('register', () => {
    it('should successfully register a new user', async () => {
      const mockGraphQLResponse = {
        register: {
          token: 'access-token-789',
          refreshToken: 'refresh-token-012',
          username: 'newuser',
          displayName: 'New User',
        },
      };

      vi.mocked(graphqlClient.request).mockResolvedValueOnce(mockGraphQLResponse);

      const data: RegisterData = {
        username: 'newuser',
        email: 'new@example.com',
        displayName: 'New User',
        password: 'SecurePass123',
      };

      const result = await authAPI.register(data);

      expect(graphqlClient.request).toHaveBeenCalledWith(
        expect.any(String), // The REGISTER_MUTATION
        {
          input: {
            username: data.username,
            email: data.email,
            password: data.password,
            displayName: data.displayName,
          },
        }
      );

      expect(result).toEqual({
        user: {
          username: 'newuser',
          displayName: 'New User',
        },
        accessToken: 'access-token-789',
        refreshToken: 'refresh-token-012',
        expiresIn: expect.any(Number),
      });

      expect(setAuthToken).toHaveBeenCalledWith('access-token-789');
    });

    it('should throw error when username is taken', async () => {
      const mockError = {
        response: {
          errors: [
            {
              message: 'Username already exists',
              extensions: {
                code: 'USERNAME_TAKEN',
              },
            },
          ],
        },
      };

      vi.mocked(graphqlClient.request).mockRejectedValueOnce(mockError);

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
      const mockGraphQLResponse = {
        refreshToken: {
          token: 'new-access-token',
          refreshToken: 'new-refresh-token',
          username: 'testuser',
          displayName: 'Test User',
        },
      };

      vi.mocked(graphqlClient.request).mockResolvedValueOnce(mockGraphQLResponse);

      const result = await authAPI.refreshToken('old-refresh-token');

      expect(graphqlClient.request).toHaveBeenCalledWith(
        expect.any(String), // The REFRESH_TOKEN_MUTATION
        { refreshToken: 'old-refresh-token' }
      );

      expect(result.accessToken).toBe('new-access-token');
      expect(setAuthToken).toHaveBeenCalledWith('new-access-token');
    });

    it('should throw error on invalid refresh token', async () => {
      const mockError = {
        response: {
          errors: [
            {
              message: 'Invalid refresh token',
            },
          ],
        },
      };

      vi.mocked(graphqlClient.request).mockRejectedValueOnce(mockError);

      await expect(authAPI.refreshToken('invalid-token')).rejects.toThrow(
        'Invalid refresh token'
      );
    });
  });

  describe('logout', () => {
    it('should successfully logout', async () => {
      await authAPI.logout();

      expect(clearAuthToken).toHaveBeenCalled();
    });
  });

  describe('validateToken', () => {
    it('should return true for valid token', async () => {
      const mockGraphQLResponse = {
        validateToken: true,
      };

      vi.mocked(graphqlClient.request).mockResolvedValueOnce(mockGraphQLResponse);

      const result = await authAPI.validateToken('valid-token');

      expect(result).toBe(true);
      expect(graphqlClient.setHeader).toHaveBeenCalledWith(
        'Authorization',
        'Bearer valid-token'
      );
    });

    it('should return false for invalid token', async () => {
      const mockError = {
        response: {
          errors: [
            {
              message: 'Invalid token',
              extensions: {
                code: 'UNAUTHENTICATED',
              },
            },
          ],
        },
      };

      vi.mocked(graphqlClient.request).mockRejectedValueOnce(mockError);

      const result = await authAPI.validateToken('invalid-token');

      expect(result).toBe(false);
    });

    it('should return false on network error', async () => {
      vi.mocked(graphqlClient.request).mockRejectedValueOnce(
        new Error('Network error')
      );

      const result = await authAPI.validateToken('token');

      expect(result).toBe(false);
    });
  });

  describe('requestPasswordReset', () => {
    it('should successfully request password reset', async () => {
      const mockGraphQLResponse = {
        forgotPassword: true,
      };

      vi.mocked(graphqlClient.request).mockResolvedValueOnce(mockGraphQLResponse);

      await expect(
        authAPI.requestPasswordReset('test@example.com')
      ).resolves.toBeUndefined();

      expect(graphqlClient.request).toHaveBeenCalledWith(
        expect.any(String), // The FORGOT_PASSWORD_MUTATION
        { email: 'test@example.com' }
      );
    });

    it('should throw error for non-existent email', async () => {
      const mockError = {
        response: {
          errors: [
            {
              message: 'Email not found',
            },
          ],
        },
      };

      vi.mocked(graphqlClient.request).mockRejectedValueOnce(mockError);

      await expect(
        authAPI.requestPasswordReset('nonexistent@example.com')
      ).rejects.toThrow('Email not found');
    });
  });
});
