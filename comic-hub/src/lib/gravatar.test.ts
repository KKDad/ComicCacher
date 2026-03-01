import { getGravatarUrl } from './gravatar';

describe('getGravatarUrl', () => {
  it('returns a gravatar URL with the SHA-256 hash', async () => {
    const url = await getGravatarUrl('test@example.com');
    expect(url).toMatch(
      /^https:\/\/www\.gravatar\.com\/avatar\/[a-f0-9]{64}\?s=80&d=404$/,
    );
  });

  it('normalises email by trimming and lowercasing', async () => {
    const a = await getGravatarUrl('  Test@Example.COM  ');
    const b = await getGravatarUrl('test@example.com');
    expect(a).toBe(b);
  });

  it('respects custom size parameter', async () => {
    const url = await getGravatarUrl('a@b.com', 200);
    expect(url).toContain('s=200');
  });
});
