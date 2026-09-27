import { JWT_COOKIE, REFRESH_COOKIE, COOKIE_MAX_AGE } from './constants';

describe('auth constants', () => {
  it('exports JWT cookie name', () => {
    expect(JWT_COOKIE).toBe('comic-hub-jwt');
  });

  it('exports refresh cookie name', () => {
    expect(REFRESH_COOKIE).toBe('comic-hub-refresh');
  });

  it('sets cookie max age to 7 days', () => {
    expect(COOKIE_MAX_AGE).toBe(60 * 60 * 24 * 7);
  });
});
