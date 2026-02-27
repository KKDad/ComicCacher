import { POST } from './route';
import { NextRequest } from 'next/server';

function createRequest(body: Record<string, unknown>) {
  return new NextRequest('http://localhost/api/register', {
    method: 'POST',
    body: JSON.stringify(body),
    headers: { 'Content-Type': 'application/json' },
  });
}

const validInput = {
  username: 'newuser',
  email: 'new@example.com',
  displayName: 'New User',
  password: 'Password1!',
  confirmPassword: 'Password1!',
};

const registerSuccess = {
  data: {
    register: {
      token: 'jwt-token',
      refreshToken: 'refresh-token',
      username: 'newuser',
      displayName: 'New User',
    },
  },
};

describe('POST /api/register', () => {
  beforeEach(() => {
    vi.spyOn(global, 'fetch').mockResolvedValue(
      new Response(JSON.stringify(registerSuccess)),
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('returns 400 for invalid input', async () => {
    const request = createRequest({ username: '' });
    const response = await POST(request);
    expect(response.status).toBe(400);
  });

  it('returns 400 for password mismatch', async () => {
    const request = createRequest({ ...validInput, confirmPassword: 'Different1!' });
    const response = await POST(request);
    expect(response.status).toBe(400);
  });

  it('forwards to backend GraphQL', async () => {
    const request = createRequest(validInput);
    await POST(request);
    expect(global.fetch).toHaveBeenCalled();
    const [, fetchOptions] = vi.mocked(global.fetch).mock.calls[0];
    const body = JSON.parse(fetchOptions!.body as string);
    expect(body.variables.input).toEqual({
      username: 'newuser',
      email: 'new@example.com',
      displayName: 'New User',
      password: 'Password1!',
    });
  });

  it('returns 400 on GraphQL errors', async () => {
    vi.mocked(global.fetch).mockResolvedValue(
      new Response(JSON.stringify({ errors: [{ message: 'Username taken' }] })),
    );
    const request = createRequest(validInput);
    const response = await POST(request);
    expect(response.status).toBe(400);
    const json = await response.json();
    expect(json.error).toBe('Username taken');
  });

  it('returns 400 when no token in response', async () => {
    vi.mocked(global.fetch).mockResolvedValue(
      new Response(JSON.stringify({ data: { register: null } })),
    );
    const request = createRequest(validInput);
    const response = await POST(request);
    expect(response.status).toBe(400);
  });

  it('sets cookies on success', async () => {
    const request = createRequest(validInput);
    const response = await POST(request);
    expect(response.status).toBe(200);
    expect(response.cookies.get('comics-hub-jwt')?.value).toBe('jwt-token');
    expect(response.cookies.get('comics-hub-refresh')?.value).toBe('refresh-token');
  });

  it('returns user data on success', async () => {
    const request = createRequest(validInput);
    const response = await POST(request);
    const json = await response.json();
    expect(json.user).toEqual({ username: 'newuser', displayName: 'New User' });
  });
});
