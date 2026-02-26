import { describe, it, expect } from 'vitest';
import {
  loginSchema,
  registerSchema,
  forgotPasswordSchema,
} from './auth';

describe('Auth Validation Schemas', () => {
  describe('loginSchema', () => {
    it('should validate correct login credentials', () => {
      const validData = {
        username: 'testuser',
        password: 'password123',
        rememberMe: false,
      };

      const result = loginSchema.safeParse(validData);
      expect(result.success).toBe(true);
    });

    it('should accept email as username', () => {
      const validData = {
        username: 'test@example.com',
        password: 'password123',
      };

      const result = loginSchema.safeParse(validData);
      expect(result.success).toBe(true);
    });

    it('should reject empty username', () => {
      const invalidData = {
        username: '',
        password: 'password123',
      };

      const result = loginSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0].message).toContain('required');
      }
    });

    it('should reject empty password', () => {
      const invalidData = {
        username: 'testuser',
        password: '',
      };

      const result = loginSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0].message).toContain('required');
      }
    });

    it('should reject password shorter than 8 characters', () => {
      const invalidData = {
        username: 'testuser',
        password: 'short',
      };

      const result = loginSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0].message).toContain('8 characters');
      }
    });

    it('should accept missing rememberMe field', () => {
      const validData = {
        username: 'testuser',
        password: 'password123',
      };

      const result = loginSchema.safeParse(validData);
      expect(result.success).toBe(true);
    });

    it('should reject username longer than 100 characters', () => {
      const invalidData = {
        username: 'a'.repeat(101),
        password: 'password123',
      };

      const result = loginSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0].message).toContain('too long');
      }
    });
  });

  describe('registerSchema', () => {
    it('should validate correct registration data', () => {
      const validData = {
        username: 'newuser',
        email: 'new@example.com',
        displayName: 'New User',
        password: 'SecurePass123',
        confirmPassword: 'SecurePass123',
      };

      const result = registerSchema.safeParse(validData);
      expect(result.success).toBe(true);
    });

    it('should reject username shorter than 3 characters', () => {
      const invalidData = {
        username: 'ab',
        email: 'test@example.com',
        displayName: 'Test',
        password: 'SecurePass123',
        confirmPassword: 'SecurePass123',
      };

      const result = registerSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0].message).toContain('3 characters');
      }
    });

    it('should reject username with invalid characters', () => {
      const invalidData = {
        username: 'test user!',
        email: 'test@example.com',
        displayName: 'Test',
        password: 'SecurePass123',
        confirmPassword: 'SecurePass123',
      };

      const result = registerSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0].message).toContain('letters, numbers');
      }
    });

    it('should accept username with underscores and hyphens', () => {
      const validData = {
        username: 'test_user-123',
        email: 'test@example.com',
        displayName: 'Test',
        password: 'SecurePass123',
        confirmPassword: 'SecurePass123',
      };

      const result = registerSchema.safeParse(validData);
      expect(result.success).toBe(true);
    });

    it('should reject invalid email format', () => {
      const invalidData = {
        username: 'testuser',
        email: 'invalid-email',
        displayName: 'Test',
        password: 'SecurePass123',
        confirmPassword: 'SecurePass123',
      };

      const result = registerSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0].message).toContain('Invalid email');
      }
    });

    it('should reject password without lowercase letter', () => {
      const invalidData = {
        username: 'testuser',
        email: 'test@example.com',
        displayName: 'Test',
        password: 'ALLUPPERCASE123',
        confirmPassword: 'ALLUPPERCASE123',
      };

      const result = registerSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues.some((issue) =>
          issue.message.includes('lowercase')
        )).toBe(true);
      }
    });

    it('should reject password without uppercase letter', () => {
      const invalidData = {
        username: 'testuser',
        email: 'test@example.com',
        displayName: 'Test',
        password: 'alllowercase123',
        confirmPassword: 'alllowercase123',
      };

      const result = registerSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues.some((issue) =>
          issue.message.includes('uppercase')
        )).toBe(true);
      }
    });

    it('should reject password without number', () => {
      const invalidData = {
        username: 'testuser',
        email: 'test@example.com',
        displayName: 'Test',
        password: 'NoNumbersHere',
        confirmPassword: 'NoNumbersHere',
      };

      const result = registerSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues.some((issue) =>
          issue.message.includes('number')
        )).toBe(true);
      }
    });

    it('should reject mismatched passwords', () => {
      const invalidData = {
        username: 'testuser',
        email: 'test@example.com',
        displayName: 'Test',
        password: 'SecurePass123',
        confirmPassword: 'DifferentPass123',
      };

      const result = registerSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0].message).toContain("don't match");
      }
    });

    it('should reject empty display name', () => {
      const invalidData = {
        username: 'testuser',
        email: 'test@example.com',
        displayName: '',
        password: 'SecurePass123',
        confirmPassword: 'SecurePass123',
      };

      const result = registerSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0].message).toContain('required');
      }
    });

    it('should reject display name longer than 50 characters', () => {
      const invalidData = {
        username: 'testuser',
        email: 'test@example.com',
        displayName: 'a'.repeat(51),
        password: 'SecurePass123',
        confirmPassword: 'SecurePass123',
      };

      const result = registerSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0].message).toContain('too long');
      }
    });
  });

  describe('forgotPasswordSchema', () => {
    it('should validate correct email', () => {
      const validData = {
        email: 'test@example.com',
      };

      const result = forgotPasswordSchema.safeParse(validData);
      expect(result.success).toBe(true);
    });

    it('should reject empty email', () => {
      const invalidData = {
        email: '',
      };

      const result = forgotPasswordSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0].message).toContain('required');
      }
    });

    it('should reject invalid email format', () => {
      const invalidData = {
        email: 'not-an-email',
      };

      const result = forgotPasswordSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.issues[0].message).toContain('Invalid email');
      }
    });

    it('should accept various valid email formats', () => {
      const validEmails = [
        'simple@example.com',
        'user+tag@example.com',
        'user.name@example.co.uk',
        'user_name@example-domain.com',
      ];

      validEmails.forEach((email) => {
        const result = forgotPasswordSchema.safeParse({ email });
        expect(result.success).toBe(true);
      });
    });
  });
});
