import { POST } from './route';

describe('POST /api/logout', () => {
  it('returns success response', async () => {
    const response = await POST();
    const json = await response.json();
    expect(json).toEqual({ success: true });
  });

  it('clears JWT cookie with maxAge 0', async () => {
    const response = await POST();
    const jwtCookie = response.cookies.get('comic-hub-jwt');
    expect(jwtCookie?.value).toBe('');
  });

  it('clears refresh cookie with maxAge 0', async () => {
    const response = await POST();
    const refreshCookie = response.cookies.get('comic-hub-refresh');
    expect(refreshCookie?.value).toBe('');
  });
});
