import { baseNavItems, isNavActive } from './nav-items';

describe('nav-items', () => {
  it('does not include the /api route (there is no page there)', () => {
    expect(baseNavItems.map((i) => i.href)).not.toContain('/api');
  });

  describe('isNavActive', () => {
    it('matches the root only exactly', () => {
      expect(isNavActive('/', '/')).toBe(true);
      expect(isNavActive('/comics', '/')).toBe(false);
    });

    it('matches a section and its children', () => {
      expect(isNavActive('/comics', '/comics')).toBe(true);
      expect(isNavActive('/comics/5', '/comics')).toBe(true);
    });

    it('does not match a sibling that shares a prefix', () => {
      expect(isNavActive('/comics-archive', '/comics')).toBe(false);
    });
  });
});
