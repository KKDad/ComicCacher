import { describe, it, expect, vi, beforeEach } from 'vitest';
import { getAuthHeaders, setAuthToken, clearAuthToken, graphqlClient } from './graphql-client';

vi.mock('graphql-request', () => {
  return {
    GraphQLClient: class {
      setHeader = vi.fn();
    },
  };
});

describe('graphql-client', () => {
  describe('getAuthHeaders', () => {
    it('returns empty object when token is undefined', () => {
      const result = getAuthHeaders();
      expect(result).toEqual({});
    });

    it('returns empty object when token is null', () => {
      const result = getAuthHeaders(null);
      expect(result).toEqual({});
    });

    it('returns Authorization header when token is provided', () => {
      const token = 'test-token-123';
      const result = getAuthHeaders(token);
      expect(result).toEqual({
        Authorization: `Bearer ${token}`,
      });
    });

    it('returns Authorization header with Bearer prefix', () => {
      const token = 'abc123xyz';
      const result = getAuthHeaders(token);
      expect(result.Authorization).toBe('Bearer abc123xyz');
    });
  });

  describe('setAuthToken', () => {
    beforeEach(() => {
      vi.clearAllMocks();
    });

    it('sets Authorization header on graphqlClient', () => {
      const token = 'test-token-456';
      const mockSetHeader = vi.fn();
      (graphqlClient as any).setHeader = mockSetHeader;

      setAuthToken(token);

      expect(mockSetHeader).toHaveBeenCalledWith('Authorization', `Bearer ${token}`);
      expect(mockSetHeader).toHaveBeenCalledTimes(1);
    });
  });

  describe('clearAuthToken', () => {
    beforeEach(() => {
      vi.clearAllMocks();
    });

    it('clears Authorization header by setting empty string', () => {
      const mockSetHeader = vi.fn();
      (graphqlClient as any).setHeader = mockSetHeader;

      clearAuthToken();

      expect(mockSetHeader).toHaveBeenCalledWith('Authorization', '');
      expect(mockSetHeader).toHaveBeenCalledTimes(1);
    });
  });
});
