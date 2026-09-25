import { POST } from './route';
import { NextRequest } from 'next/server';

function createRequest(body: Record<string, unknown>) {
  return new NextRequest('http://localhost/api/reset-password', {
    method: 'POST',
    body: JSON.stringify(body),
    headers: { 'Content-Type': 'application/json' },
  });
}

const validInput = { token: 'tok', password: 'NewPass123', confirmPassword: 'NewPass123' };

const resetSuccess = {
  data: {
    resetPassword: {
      token: 'jwt-token',
      refreshToken: 'refresh-token',
      username: 'user',
      displayName: 'User',
    },
  },
};

describe('POST /api/reset-password', () => {
  beforeEach(() => {
    vi.spyOn(global, 'fetch').mockResolvedValue(new Response(JSON.stringify(resetSuccess)));
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('returns 400 for a weak password', async () => {
    const response = await POST(createRequest({ ...validInput, password: 'short', confirmPassword: 'short' }));
    expect(response.status).toBe(400);
    expect(global.fetch).not.toHaveBeenCalled();
  });

  it('returns 400 for a missing token', async () => {
    const response = await POST(createRequest({ ...validInput, token: '' }));
    expect(response.status).toBe(400);
  });

  it('forwards the token and new password to the backend', async () => {
    await POST(createRequest(validInput));
    const [, options] = vi.mocked(global.fetch).mock.calls[0];
    const body = JSON.parse(options!.body as string);
    expect(body.query).toContain('resetPassword');
    expect(body.variables).toEqual({ token: 'tok', newPassword: 'NewPass123' });
  });

  it('signs the user in by setting auth cookies', async () => {
    const response = await POST(createRequest(validInput));
    expect(response.status).toBe(200);
    expect(response.cookies.get('comic-hub-jwt')?.value).toBe('jwt-token');
    expect(response.cookies.get('comic-hub-refresh')?.value).toBe('refresh-token');
    expect(response.cookies.get('comic-hub-jwt')?.httpOnly).toBe(true);
  });

  it('reports an expired link on GraphQL errors', async () => {
    vi.mocked(global.fetch).mockResolvedValue(
      new Response(JSON.stringify({ errors: [{ message: 'Password reset failed' }] })),
    );
    const response = await POST(createRequest(validInput));
    expect(response.status).toBe(400);
    expect((await response.json()).error).toMatch(/invalid or has expired/);
  });

  it('reports an expired link when no token comes back', async () => {
    vi.mocked(global.fetch).mockResolvedValue(new Response(JSON.stringify({ data: { resetPassword: null } })));
    const response = await POST(createRequest(validInput));
    expect(response.status).toBe(400);
  });
});
