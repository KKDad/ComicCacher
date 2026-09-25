import { comicTitle } from './comic-title';
import { getAuthenticatedClient } from '@/lib/auth/graphql-server';

vi.mock('@/lib/auth/graphql-server', () => ({
  getAuthenticatedClient: vi.fn(),
}));

function mockRequest(impl: () => Promise<unknown>) {
  const request = vi.fn().mockImplementation(impl);
  vi.mocked(getAuthenticatedClient).mockResolvedValue({ request } as any);
  return request;
}

describe('comicTitle', () => {
  it('returns the comic name', async () => {
    const request = mockRequest(async () => ({ comic: { name: 'Garfield' } }));
    expect(await comicTitle('7')).toBe('Garfield');
    expect(request).toHaveBeenCalledWith(expect.any(String), { id: 7 });
  });

  it('falls back when the comic does not exist', async () => {
    mockRequest(async () => ({ comic: null }));
    expect(await comicTitle('7')).toBe('Comic');
  });

  it('falls back without calling the backend for a non-numeric id', async () => {
    const request = mockRequest(async () => ({ comic: { name: 'x' } }));
    expect(await comicTitle('abc')).toBe('Comic');
    expect(request).not.toHaveBeenCalled();
  });

  it('falls back when the request fails', async () => {
    mockRequest(async () => { throw new Error('401'); });
    expect(await comicTitle('7')).toBe('Comic');
  });
});
