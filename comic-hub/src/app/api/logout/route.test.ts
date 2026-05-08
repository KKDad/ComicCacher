import { POST } from './route';

const requestMock = vi.fn();

vi.mock('@/lib/auth/graphql-server', () => ({
  getAuthenticatedClient: vi.fn(() => Promise.resolve({ request: requestMock })),
}));

describe('POST /api/logout', () => {
  beforeEach(() => {
    requestMock.mockReset();
    vi.spyOn(console, 'warn').mockImplementation(() => {});
  });

  it('calls the logout mutation and clears cookies on success', async () => {
    requestMock.mockResolvedValueOnce({ logout: true });

    const response = await POST();
    const json = await response.json();

    expect(requestMock).toHaveBeenCalledTimes(1);
    expect(json).toEqual({ success: true });
    expect(response.cookies.get('comic-hub-jwt')?.value).toBe('');
    expect(response.cookies.get('comic-hub-refresh')?.value).toBe('');
  });

  it('still clears cookies when the logout mutation fails', async () => {
    requestMock.mockRejectedValueOnce(new Error('backend down'));

    const response = await POST();
    const json = await response.json();

    expect(requestMock).toHaveBeenCalledTimes(1);
    expect(json).toEqual({ success: true });
    expect(response.cookies.get('comic-hub-jwt')?.value).toBe('');
    expect(response.cookies.get('comic-hub-refresh')?.value).toBe('');
  });
});
