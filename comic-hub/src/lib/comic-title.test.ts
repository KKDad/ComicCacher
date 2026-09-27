import { comicTitle } from './comic-title';
import { getAuthenticatedClient } from '@/lib/auth/graphql-server';

vi.mock('@/lib/auth/graphql-server', () => ({
  getAuthenticatedClient: vi.fn(),
}));

type Client = Awaited<ReturnType<typeof getAuthenticatedClient>>;

function mockRequest(impl: () => Promise<unknown>) {
  const request = vi.fn(impl);
  vi.mocked(getAuthenticatedClient).mockResolvedValue({ request } as unknown as Client);
  return request;
}

describe('comicTitle', () => {
  it('returns the comic name', async () => {
    const request = mockRequest(async () => ({ comic: { name: 'Adam At Home' } }));

    await expect(comicTitle('42')).resolves.toBe('Adam At Home');
    expect(request).toHaveBeenCalledWith(expect.any(String), { id: 42 });
  });

  it('returns undefined for an id that is not a number, without a request', async () => {
    const request = mockRequest(async () => ({}));

    await expect(comicTitle('abc')).resolves.toBeUndefined();
    expect(request).not.toHaveBeenCalled();
  });

  it('returns undefined for an unknown comic', async () => {
    mockRequest(async () => ({ comic: null }));
    await expect(comicTitle('1')).resolves.toBeUndefined();
  });

  it('returns undefined when the lookup fails', async () => {
    mockRequest(async () => {
      throw new Error('401');
    });
    await expect(comicTitle('1')).resolves.toBeUndefined();
  });
});
