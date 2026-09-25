import { safeRedirectPath } from './safe-redirect';

describe('safeRedirectPath', () => {
  it('keeps same-origin paths', () => {
    expect(safeRedirectPath('/comics/5/read?date=2026-01-01')).toBe('/comics/5/read?date=2026-01-01');
  });

  it.each([
    ['https://evil.example'],
    ['//evil.example'],
    ['/\\evil.example'],
    ['javascript:alert(1)'],
    ['comics'],
    [''],
    [null],
    [undefined],
  ])('falls back for %s', (target) => {
    expect(safeRedirectPath(target)).toBe('/');
  });

  it('uses the given fallback', () => {
    expect(safeRedirectPath('https://evil.example', '/read')).toBe('/read');
  });
});
