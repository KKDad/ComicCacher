import manifest from './manifest';

describe('manifest', () => {
  it('names the app and points at its home-screen icons', () => {
    const m = manifest();
    expect(m.name).toBe('Comics Hub');
    expect(m.start_url).toBe('/');
    expect(m.id).toBe('/');
    expect(m.scope).toBe('/');
    expect(m.display).toBe('standalone');
    expect(m.icons?.map((i) => i.sizes)).toEqual(['192x192', '512x512']);
  });
});
