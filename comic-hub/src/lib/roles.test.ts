import { hasRole, isOperator, isAdmin } from './roles';

describe('roles', () => {
  describe('hasRole', () => {
    it('USER satisfies USER requirement', () => {
      expect(hasRole(['USER'], 'USER')).toBe(true);
    });

    it('USER does not satisfy OPERATOR requirement', () => {
      expect(hasRole(['USER'], 'OPERATOR')).toBe(false);
    });

    it('USER does not satisfy ADMIN requirement', () => {
      expect(hasRole(['USER'], 'ADMIN')).toBe(false);
    });

    it('OPERATOR satisfies USER requirement (hierarchy)', () => {
      expect(hasRole(['OPERATOR'], 'USER')).toBe(true);
    });

    it('OPERATOR satisfies OPERATOR requirement', () => {
      expect(hasRole(['OPERATOR'], 'OPERATOR')).toBe(true);
    });

    it('OPERATOR does not satisfy ADMIN requirement', () => {
      expect(hasRole(['OPERATOR'], 'ADMIN')).toBe(false);
    });

    it('ADMIN satisfies all requirements', () => {
      expect(hasRole(['ADMIN'], 'USER')).toBe(true);
      expect(hasRole(['ADMIN'], 'OPERATOR')).toBe(true);
      expect(hasRole(['ADMIN'], 'ADMIN')).toBe(true);
    });

    it('empty roles satisfy nothing', () => {
      expect(hasRole([], 'USER')).toBe(false);
    });

    it('unknown roles are ignored', () => {
      expect(hasRole(['UNKNOWN'], 'USER')).toBe(false);
    });
  });

  describe('isOperator', () => {
    it('returns false for USER only', () => {
      expect(isOperator(['USER'])).toBe(false);
    });

    it('returns true for OPERATOR', () => {
      expect(isOperator(['USER', 'OPERATOR'])).toBe(true);
    });

    it('returns true for ADMIN (hierarchy)', () => {
      expect(isOperator(['USER', 'ADMIN'])).toBe(true);
    });
  });

  describe('isAdmin', () => {
    it('returns false for USER only', () => {
      expect(isAdmin(['USER'])).toBe(false);
    });

    it('returns false for OPERATOR only', () => {
      expect(isAdmin(['OPERATOR'])).toBe(false);
    });

    it('returns true for ADMIN', () => {
      expect(isAdmin(['USER', 'ADMIN'])).toBe(true);
    });
  });
});
