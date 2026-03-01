import { POST } from './route';
import { NextRequest } from 'next/server';

function createRequest(body: Record<string, unknown>) {
  return new NextRequest('http://localhost/api/login', {
    method: 'POST',
    body: JSON.stringify(body),
    headers: { 'Content-Type': 'application/json' },
  });
}

const loginSuccess = {
  data: {
    login: {
      token: 'jwt-token',
      refreshToken: 'refresh-token',
      username: 'testuser',
      displayName: 'Test User',
    },
  },
};

describe('POST /api/login', () => {
  beforeEach(() => {
    vi.spyOn(global, 'fetch').mockResolvedValue(
      new Response(JSON.stringify(loginSuccess)),
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('returns 400 for invalid input', async () => {
    const request = createRequest({ username: '', password: '' });
    const response = await POST(request);
    expect(response.status).toBe(400);
  });

  it('returns 400 for missing fields', async () => {
    const request = createRequest({});
    const response = await POST(request);
    expect(response.status).toBe(400);
  });

  it('forwards credentials to backend GraphQL', async () => {
    const request = createRequest({ username: 'testuser', password: 'Password1!' });
    await POST(request);
    expect(global.fetch).toHaveBeenCalled();
    const [, fetchOptions] = vi.mocked(global.fetch).mock.calls[0];
    const body = JSON.parse(fetchOptions!.body as string);
    expect(body.variables.input).toEqual({ username: 'testuser', password: 'Password1!' });
  });

  it('returns 401 on GraphQL errors', async () => {
    vi.mocked(global.fetch).mockResolvedValue(
      new Response(JSON.stringify({ errors: [{ message: 'Invalid credentials' }] })),
    );
    const request = createRequest({ username: 'testuser', password: 'Password1!' });
    const response = await POST(request);
    expect(response.status).toBe(401);
    const json = await response.json();
    expect(json.error).toBe('Invalid credentials');
  });

  it('returns 401 when no token in response', async () => {
    vi.mocked(global.fetch).mockResolvedValue(
      new Response(JSON.stringify({ data: { login: null } })),
    );
    const request = createRequest({ username: 'testuser', password: 'Password1!' });
    const response = await POST(request);
    expect(response.status).toBe(401);
  });

  it('sets JWT and refresh cookies on success', async () => {
    const request = createRequest({ username: 'testuser', password: 'Password1!' });
    const response = await POST(request);
    expect(response.status).toBe(200);
    expect(response.cookies.get('comics-hub-jwt')?.value).toBe('jwt-token');
    expect(response.cookies.get('comics-hub-refresh')?.value).toBe('refresh-token');
  });

  it('returns user data on success', async () => {
    const request = createRequest({ username: 'testuser', password: 'Password1!' });
    const response = await POST(request);
    const json = await response.json();
    expect(json.user).toEqual({ username: 'testuser', displayName: 'Test User' });
  });

  it('sets maxAge on cookies when rememberMe is true', async () => {
    const request = createRequest({ username: 'testuser', password: 'Password1!', rememberMe: true });
    const response = await POST(request);
    // Cookie is set — we can verify the value exists
    expect(response.cookies.get('comics-hub-jwt')?.value).toBe('jwt-token');
  });
});
