import { formatBytes } from './format';

describe('formatBytes', () => {
  it.each([
    [0, '0 B'],
    [512, '512 B'],
    [1536, '1.5 KB'],
    [5 * 1024 * 1024, '5.0 MB'],
  ])('formats %d as %s', (bytes, expected) => {
    expect(formatBytes(bytes)).toBe(expected);
  });
});
