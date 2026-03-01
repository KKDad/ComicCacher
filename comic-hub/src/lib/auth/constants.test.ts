import { JWT_COOKIE, REFRESH_COOKIE, COOKIE_MAX_AGE, PUBLIC_PATHS } from './constants';

describe('auth constants', () => {
  it('exports JWT cookie name', () => {
    expect(JWT_COOKIE).toBe('comics-hub-jwt');
  });

  it('exports refresh cookie name', () => {
    expect(REFRESH_COOKIE).toBe('comics-hub-refresh');
  });

  it('sets cookie max age to 7 days', () => {
    expect(COOKIE_MAX_AGE).toBe(60 * 60 * 24 * 7);
  });

  it('exports public paths', () => {
    expect(PUBLIC_PATHS).toContain('/login');
    expect(PUBLIC_PATHS).toContain('/register');
    expect(PUBLIC_PATHS).toContain('/forgot-password');
  });
});
