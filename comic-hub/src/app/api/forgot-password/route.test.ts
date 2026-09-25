import { POST } from './route';
import { NextRequest } from 'next/server';

function createRequest(body: Record<string, unknown>) {
  return new NextRequest('http://localhost/api/forgot-password', {
    method: 'POST',
    body: JSON.stringify(body),
    headers: { 'Content-Type': 'application/json' },
  });
}

describe('POST /api/forgot-password', () => {
  beforeEach(() => {
    vi.spyOn(global, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ data: { forgotPassword: true } })),
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('returns 400 for an invalid email', async () => {
    const response = await POST(createRequest({ email: 'nope' }));
    expect(response.status).toBe(400);
    expect(global.fetch).not.toHaveBeenCalled();
  });

  it('forwards the forgotPassword mutation to the backend', async () => {
    await POST(createRequest({ email: 'a@example.com' }));
    const [, options] = vi.mocked(global.fetch).mock.calls[0];
    const body = JSON.parse(options!.body as string);
    expect(body.query).toContain('forgotPassword');
    expect(body.variables).toEqual({ email: 'a@example.com' });
  });

  it('returns ok when the backend accepts the request', async () => {
    const response = await POST(createRequest({ email: 'a@example.com' }));
    expect(response.status).toBe(200);
    expect(await response.json()).toEqual({ ok: true });
  });

  it('returns 502 without leaking backend detail on GraphQL errors', async () => {
    vi.mocked(global.fetch).mockResolvedValue(
      new Response(JSON.stringify({ errors: [{ message: 'SMTP connect failed to 10.0.0.5' }] })),
    );
    const response = await POST(createRequest({ email: 'a@example.com' }));
    expect(response.status).toBe(502);
    const json = await response.json();
    expect(json.error).not.toContain('10.0.0.5');
  });
});
