import { getSession } from './session';
import { mockCookieStore } from '@/test/mock-cookies';

vi.mock('next/headers', () => ({
  cookies: vi.fn(),
}));

const mockUser = {
  username: 'testuser',
  email: 'test@example.com',
  displayName: 'Test User',
  created: '2024-01-01',
  roles: ['USER'],
};

describe('getSession', () => {
  beforeEach(() => {
    vi.spyOn(global, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ data: { me: mockUser } })),
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('returns null when no JWT cookie', async () => {
    mockCookieStore({});
    const result = await getSession();
    expect(result).toBeNull();
    expect(global.fetch).not.toHaveBeenCalled();
  });

  it('fetches user with Bearer token', async () => {
    mockCookieStore({ 'comic-hub-jwt': 'test-jwt' });
    await getSession();
    expect(global.fetch).toHaveBeenCalled();
    const [, fetchOptions] = vi.mocked(global.fetch).mock.calls[0];
    expect((fetchOptions!.headers as Record<string, string>).Authorization).toBe('Bearer test-jwt');
  });

  it('returns user on success', async () => {
    mockCookieStore({ 'comic-hub-jwt': 'test-jwt' });
    const result = await getSession();
    expect(result).toEqual(mockUser);
  });

  it('returns null on non-ok response', async () => {
    mockCookieStore({ 'comic-hub-jwt': 'test-jwt' });
    vi.mocked(global.fetch).mockResolvedValue(new Response('Error', { status: 500 }));
    const result = await getSession();
    expect(result).toBeNull();
  });

  it('returns null when response data is missing me field', async () => {
    mockCookieStore({ 'comic-hub-jwt': 'test-jwt' });
    vi.mocked(global.fetch).mockResolvedValue(
      new Response(JSON.stringify({ data: { me: null } })),
    );
    const result = await getSession();
    expect(result).toBeNull();
  });

  it('returns null when response has no data field', async () => {
    mockCookieStore({ 'comic-hub-jwt': 'test-jwt' });
    vi.mocked(global.fetch).mockResolvedValue(
      new Response(JSON.stringify({ errors: [{ message: 'Unauthorized' }] })),
    );
    const result = await getSession();
    expect(result).toBeNull();
  });

  it('returns null on fetch error', async () => {
    mockCookieStore({ 'comic-hub-jwt': 'test-jwt' });
    vi.mocked(global.fetch).mockRejectedValue(new Error('Network error'));
    const result = await getSession();
    expect(result).toBeNull();
  });
});
