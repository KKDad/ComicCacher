import { render, renderHook } from '@testing-library/react';
import { usePathname } from 'next/navigation';
import { mockRouter } from '@/test/mock-next';
import { NavigationTracker, hasInAppHistory, resetNavigationHistory, useGoBack } from './navigation-history';

describe('navigation-history', () => {
  beforeEach(() => {
    resetNavigationHistory();
    vi.mocked(usePathname).mockReturnValue('/comics/1/read');
  });

  it('starts with no in-app history (the page was opened directly)', () => {
    render(<NavigationTracker />);
    expect(hasInAppHistory()).toBe(false);
  });

  it('records a route change', () => {
    const { rerender } = render(<NavigationTracker />);

    vi.mocked(usePathname).mockReturnValue('/comics');
    rerender(<NavigationTracker />);

    expect(hasInAppHistory()).toBe(true);
  });

  it('ignores re-renders on the same route', () => {
    const { rerender } = render(<NavigationTracker />);
    rerender(<NavigationTracker />);
    expect(hasInAppHistory()).toBe(false);
  });

  describe('useGoBack', () => {
    it('goes to the fallback when the page was opened directly', () => {
      const router = mockRouter();
      const { result } = renderHook(() => useGoBack('/comics'));

      result.current();

      expect(router.push).toHaveBeenCalledWith('/comics');
      expect(router.back).not.toHaveBeenCalled();
    });

    it('goes back after an in-app navigation', () => {
      const router = mockRouter();
      const { rerender } = render(<NavigationTracker />);
      vi.mocked(usePathname).mockReturnValue('/comics');
      rerender(<NavigationTracker />);

      const { result } = renderHook(() => useGoBack('/comics'));
      result.current();

      expect(router.back).toHaveBeenCalledOnce();
      expect(router.push).not.toHaveBeenCalled();
    });
  });
});
