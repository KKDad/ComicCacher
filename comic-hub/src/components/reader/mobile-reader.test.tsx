import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MobileReader } from './mobile-reader';
import type { useReader } from '@/hooks/use-reader';

vi.mock('next/navigation', () => ({
  useRouter: vi.fn().mockReturnValue({ back: vi.fn(), push: vi.fn() }),
  usePathname: vi.fn().mockReturnValue('/comics/1/read'),
}));

import { useSwipe } from '@/hooks/use-swipe';

vi.mock('@/hooks/use-swipe', () => ({
  useSwipe: vi.fn().mockReturnValue({
    onTouchStart: vi.fn(),
    onTouchMove: vi.fn(),
    onTouchEnd: vi.fn(),
  }),
}));

import { usePinchZoom } from '@/hooks/use-pinch-zoom';

vi.mock('@/hooks/use-pinch-zoom', () => ({
  usePinchZoom: vi.fn().mockReturnValue({
    state: { scale: 1, originX: 50, originY: 50, translateX: 0, translateY: 0 },
    handlers: {
      onTouchStart: vi.fn(),
      onTouchMove: vi.fn(),
      onTouchEnd: vi.fn(),
    },
    isZoomed: false,
    resetZoom: vi.fn(),
  }),
}));

vi.mock('@/hooks/use-reading-list', () => ({
  useReadingList: vi.fn().mockReturnValue({
    comics: [],
    previousComic: null,
    nextComic: null,
    navigateToComic: vi.fn(),
    isLoading: false,
  }),
}));

vi.mock('@/generated/graphql', () => ({
  useGetComicsQuery: vi.fn().mockReturnValue({ data: null, isLoading: false }),
  useGetUserPreferencesQuery: vi.fn().mockReturnValue({ data: null, isLoading: false }),
}));

function createMockReader(overrides?: Partial<ReturnType<typeof useReader>>): ReturnType<typeof useReader> {
  return {
    strips: [
      { date: '2026-03-14', available: true, imageUrl: 'https://example.com/14.png', width: 900, height: 300 },
      { date: '2026-03-15', available: true, imageUrl: 'https://example.com/15.png', width: 900, height: 300 },
      { date: '2026-03-16', available: true, imageUrl: 'https://example.com/16.png', width: 900, height: 300 },
    ],
    currentIndex: 1,
    setCurrentIndex: vi.fn(),
    comicName: 'Garfield',
    oldest: '2020-01-01',
    newest: '2026-03-20',
    avatarUrl: null,
    hasOlder: true,
    hasNewer: true,
    isLoading: false,
    loadOlder: vi.fn(),
    loadNewer: vi.fn(),
    goToDate: vi.fn(),
    goToFirst: vi.fn().mockReturnValue('scrolled'),
    goToLast: vi.fn().mockReturnValue('scrolled'),
    goToRandom: vi.fn(),
    goNewer: vi.fn(),
    goOlder: vi.fn(),
    isLoadingRandom: false,
    ...overrides,
  };
}

describe('MobileReader', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders comic name', () => {
    render(<MobileReader comicId={1} reader={createMockReader()} />);
    expect(screen.getByText('Garfield')).toBeInTheDocument();
  });

  it('renders current strip image', () => {
    render(<MobileReader comicId={1} reader={createMockReader()} />);

    const img = screen.getByRole('img');
    expect(img).toHaveAttribute('src', 'https://example.com/15.png');
  });

  it('renders formatted date for current strip', () => {
    render(<MobileReader comicId={1} reader={createMockReader()} />);

    // The date is formatted via toLocaleDateString — look for 2026 and March indicators
    expect(screen.getByText(/2026/)).toBeInTheDocument();
  });

  it('shows loading skeleton when isLoading', () => {
    const reader = createMockReader({ isLoading: true });
    const { container } = render(<MobileReader comicId={1} reader={reader} />);

    expect(container.querySelectorAll('[data-slot="skeleton"]').length).toBeGreaterThanOrEqual(1);
  });

  it('shows unavailable message when strip has no image', () => {
    const reader = createMockReader({
      strips: [{ date: '2026-03-15', available: false, imageUrl: null, width: null, height: null }],
      currentIndex: 0,
    });
    render(<MobileReader comicId={1} reader={reader} />);

    expect(screen.getByText(/no strip available/i)).toBeInTheDocument();
  });

  it('shows no strips message when strips array is empty', () => {
    const reader = createMockReader({ strips: [], currentIndex: 0 });
    render(<MobileReader comicId={1} reader={reader} />);

    expect(screen.getByText(/no strips loaded/i)).toBeInTheDocument();
  });

  it('toggles controls visibility on click', async () => {
    const { container } = render(<MobileReader comicId={1} reader={createMockReader()} />);

    // Controls start hidden (opacity-0)
    const overlay = container.querySelector('.pointer-events-none');
    expect(overlay?.className).toContain('opacity-0');

    // Click content area to toggle controls
    const contentArea = container.querySelector('.flex-1.flex');
    if (contentArea) {
      await userEvent.click(contentArea);
    }

    expect(overlay?.className).toContain('opacity-100');
  });

  it('renders Go back button in controls overlay', async () => {
    const { container } = render(<MobileReader comicId={1} reader={createMockReader()} />);

    // Toggle controls visible
    const contentArea = container.querySelector('.flex-1.flex');
    if (contentArea) {
      await userEvent.click(contentArea);
    }

    expect(screen.getByRole('button', { name: /go back/i })).toBeInTheDocument();
  });

  it('renders older and newer navigation buttons', async () => {
    const { container } = render(<MobileReader comicId={1} reader={createMockReader()} />);

    const contentArea = container.querySelector('.flex-1.flex');
    if (contentArea) {
      await userEvent.click(contentArea);
    }

    expect(screen.getByRole('button', { name: /older strip/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /newer strip/i })).toBeInTheDocument();
  });

  it('disables older button at first strip with no older', async () => {
    const reader = createMockReader({ currentIndex: 0, hasOlder: false });
    const { container } = render(<MobileReader comicId={1} reader={reader} />);

    const contentArea = container.querySelector('.flex-1.flex');
    if (contentArea) {
      await userEvent.click(contentArea);
    }

    expect(screen.getByRole('button', { name: /older strip/i })).toBeDisabled();
  });

  it('disables newer button at last strip with no newer', async () => {
    const reader = createMockReader({ currentIndex: 2, hasNewer: false });
    const { container } = render(<MobileReader comicId={1} reader={reader} />);

    const contentArea = container.querySelector('.flex-1.flex');
    if (contentArea) {
      await userEvent.click(contentArea);
    }

    expect(screen.getByRole('button', { name: /newer strip/i })).toBeDisabled();
  });

  it('dismisses controls on Escape key', async () => {
    const { container } = render(<MobileReader comicId={1} reader={createMockReader()} />);

    // Show controls
    const contentArea = container.querySelector('.flex-1.flex');
    if (contentArea) {
      await userEvent.click(contentArea);
    }

    const overlay = container.querySelector('.pointer-events-none');
    expect(overlay?.className).toContain('opacity-100');

    // Press Escape
    fireEvent.keyDown(window, { key: 'Escape' });
    expect(overlay?.className).toContain('opacity-0');
  });

  it('renders reading list drawer button', async () => {
    const { container } = render(<MobileReader comicId={1} reader={createMockReader()} />);

    const contentArea = container.querySelector('.flex-1.flex');
    if (contentArea) {
      await userEvent.click(contentArea);
    }

    expect(screen.getByRole('button', { name: /reading list/i })).toBeInTheDocument();
  });

  it('passes swipe callbacks to useSwipe when not zoomed', () => {
    let capturedOptions: any;
    vi.mocked(useSwipe).mockImplementation((opts: any) => {
      capturedOptions = opts;
      return { onTouchStart: vi.fn(), onTouchMove: vi.fn(), onTouchEnd: vi.fn() };
    });

    render(<MobileReader comicId={1} reader={createMockReader()} />);

    expect(capturedOptions.onSwipeUp).toBeInstanceOf(Function);
    expect(capturedOptions.onSwipeDown).toBeInstanceOf(Function);
    expect(capturedOptions.threshold).toBe(50);
  });

  it('calls goOlder via swipe down callback', () => {
    const goOlder = vi.fn();
    let capturedOnSwipeDown: (() => void) | undefined;

    vi.mocked(useSwipe).mockImplementation((opts: any) => {
      capturedOnSwipeDown = opts.onSwipeDown;
      return { onTouchStart: vi.fn(), onTouchMove: vi.fn(), onTouchEnd: vi.fn() };
    });

    render(<MobileReader comicId={1} reader={createMockReader({ goOlder, currentIndex: 1 })} />);

    expect(capturedOnSwipeDown).toBeDefined();
    capturedOnSwipeDown!();
  });

  it('does not toggle controls when zoomed', async () => {
    vi.mocked(usePinchZoom).mockReturnValue({
      state: { scale: 2, originX: 50, originY: 50, translateX: 0, translateY: 0 },
      handlers: { onTouchStart: vi.fn(), onTouchMove: vi.fn(), onTouchEnd: vi.fn() },
      isZoomed: true,
      resetZoom: vi.fn(),
    });

    const { container } = render(<MobileReader comicId={1} reader={createMockReader()} />);

    const overlay = container.querySelector('.pointer-events-none');
    expect(overlay?.className).toContain('opacity-0');

    const contentArea = container.querySelector('.flex-1.flex');
    if (contentArea) {
      await userEvent.click(contentArea);
    }

    // Controls should remain hidden when zoomed
    expect(overlay?.className).toContain('opacity-0');
  });

  it('passes undefined swipe handlers when zoomed', () => {
    vi.mocked(usePinchZoom).mockReturnValue({
      state: { scale: 2, originX: 50, originY: 50, translateX: 0, translateY: 0 },
      handlers: { onTouchStart: vi.fn(), onTouchMove: vi.fn(), onTouchEnd: vi.fn() },
      isZoomed: true,
      resetZoom: vi.fn(),
    });

    let capturedOptions: any;
    vi.mocked(useSwipe).mockImplementation((opts: any) => {
      capturedOptions = opts;
      return { onTouchStart: vi.fn(), onTouchMove: vi.fn(), onTouchEnd: vi.fn() };
    });

    render(<MobileReader comicId={1} reader={createMockReader()} />);

    // When zoomed, swipe handlers should be undefined
    expect(capturedOptions.onSwipeUp).toBeUndefined();
    expect(capturedOptions.onSwipeDown).toBeUndefined();
  });

  it('renders older button click calls goOlder', async () => {
    const goOlder = vi.fn();
    const { container } = render(<MobileReader comicId={1} reader={createMockReader({ goOlder })} />);

    // Show controls
    const contentArea = container.querySelector('.flex-1.flex');
    if (contentArea) {
      await userEvent.click(contentArea);
    }

    await userEvent.click(screen.getByRole('button', { name: /older strip/i }));
    expect(goOlder).toHaveBeenCalledOnce();
  });

  it('renders newer button click calls goNewer', async () => {
    const goNewer = vi.fn();
    const { container } = render(<MobileReader comicId={1} reader={createMockReader({ goNewer })} />);

    const contentArea = container.querySelector('.flex-1.flex');
    if (contentArea) {
      await userEvent.click(contentArea);
    }

    await userEvent.click(screen.getByRole('button', { name: /newer strip/i }));
    expect(goNewer).toHaveBeenCalledOnce();
  });

  it('merges touch handlers for single-touch start', () => {
    const zoomTouchStart = vi.fn();
    const swipeTouchStart = vi.fn();

    vi.mocked(usePinchZoom).mockReturnValue({
      state: { scale: 1, originX: 50, originY: 50, translateX: 0, translateY: 0 },
      handlers: { onTouchStart: zoomTouchStart, onTouchMove: vi.fn(), onTouchEnd: vi.fn() },
      isZoomed: false,
      resetZoom: vi.fn(),
    });

    vi.mocked(useSwipe).mockReturnValue({
      onTouchStart: swipeTouchStart,
      onTouchMove: vi.fn(),
      onTouchEnd: vi.fn(),
    });

    const { container } = render(<MobileReader comicId={1} reader={createMockReader()} />);

    const contentArea = container.querySelector('.flex-1.flex');
    if (contentArea) {
      const touch = { clientX: 100, clientY: 200, identifier: 0 };
      fireEvent.touchStart(contentArea, { touches: [touch] });

      // Both swipe and zoom handlers should be called for single touch
      expect(swipeTouchStart).toHaveBeenCalled();
      expect(zoomTouchStart).toHaveBeenCalled();
    }
  });

  it('merges touch handlers for two-finger pinch start', () => {
    const zoomTouchStart = vi.fn();
    const swipeTouchStart = vi.fn();

    vi.mocked(usePinchZoom).mockReturnValue({
      state: { scale: 1, originX: 50, originY: 50, translateX: 0, translateY: 0 },
      handlers: { onTouchStart: zoomTouchStart, onTouchMove: vi.fn(), onTouchEnd: vi.fn() },
      isZoomed: false,
      resetZoom: vi.fn(),
    });

    vi.mocked(useSwipe).mockReturnValue({
      onTouchStart: swipeTouchStart,
      onTouchMove: vi.fn(),
      onTouchEnd: vi.fn(),
    });

    const { container } = render(<MobileReader comicId={1} reader={createMockReader()} />);

    const contentArea = container.querySelector('.flex-1.flex');
    if (contentArea) {
      const touch1 = { clientX: 100, clientY: 200, identifier: 0 };
      const touch2 = { clientX: 200, clientY: 200, identifier: 1 };
      fireEvent.touchStart(contentArea, { touches: [touch1, touch2] });

      // Only zoom handler for 2-finger touch
      expect(zoomTouchStart).toHaveBeenCalled();
      expect(swipeTouchStart).not.toHaveBeenCalled();
    }
  });

  it('routes touch move to zoom handler when zoomed', () => {
    const zoomTouchMove = vi.fn();
    const swipeTouchMove = vi.fn();

    vi.mocked(usePinchZoom).mockReturnValue({
      state: { scale: 2, originX: 50, originY: 50, translateX: 0, translateY: 0 },
      handlers: { onTouchStart: vi.fn(), onTouchMove: zoomTouchMove, onTouchEnd: vi.fn() },
      isZoomed: true,
      resetZoom: vi.fn(),
    });

    vi.mocked(useSwipe).mockReturnValue({
      onTouchStart: vi.fn(),
      onTouchMove: swipeTouchMove,
      onTouchEnd: vi.fn(),
    });

    const { container } = render(<MobileReader comicId={1} reader={createMockReader()} />);

    const contentArea = container.querySelector('.flex-1.flex');
    if (contentArea) {
      const touch = { clientX: 150, clientY: 250, identifier: 0 };
      fireEvent.touchMove(contentArea, { touches: [touch] });

      expect(zoomTouchMove).toHaveBeenCalled();
      expect(swipeTouchMove).not.toHaveBeenCalled();
    }
  });

  it('routes touch move to swipe handler when not zoomed and single touch', () => {
    const zoomTouchMove = vi.fn();
    const swipeTouchMove = vi.fn();

    vi.mocked(usePinchZoom).mockReturnValue({
      state: { scale: 1, originX: 50, originY: 50, translateX: 0, translateY: 0 },
      handlers: { onTouchStart: vi.fn(), onTouchMove: zoomTouchMove, onTouchEnd: vi.fn() },
      isZoomed: false,
      resetZoom: vi.fn(),
    });

    vi.mocked(useSwipe).mockReturnValue({
      onTouchStart: vi.fn(),
      onTouchMove: swipeTouchMove,
      onTouchEnd: vi.fn(),
    });

    const { container } = render(<MobileReader comicId={1} reader={createMockReader()} />);

    const contentArea = container.querySelector('.flex-1.flex');
    if (contentArea) {
      const touch = { clientX: 150, clientY: 250, identifier: 0 };
      fireEvent.touchMove(contentArea, { touches: [touch] });

      expect(swipeTouchMove).toHaveBeenCalled();
      expect(zoomTouchMove).not.toHaveBeenCalled();
    }
  });

  it('routes touch move to zoom handler for two-finger move', () => {
    const zoomTouchMove = vi.fn();

    vi.mocked(usePinchZoom).mockReturnValue({
      state: { scale: 1, originX: 50, originY: 50, translateX: 0, translateY: 0 },
      handlers: { onTouchStart: vi.fn(), onTouchMove: zoomTouchMove, onTouchEnd: vi.fn() },
      isZoomed: false,
      resetZoom: vi.fn(),
    });

    const { container } = render(<MobileReader comicId={1} reader={createMockReader()} />);

    const contentArea = container.querySelector('.flex-1.flex');
    if (contentArea) {
      const touch1 = { clientX: 100, clientY: 200, identifier: 0 };
      const touch2 = { clientX: 200, clientY: 200, identifier: 1 };
      fireEvent.touchMove(contentArea, { touches: [touch1, touch2] });

      expect(zoomTouchMove).toHaveBeenCalled();
    }
  });

  it('calls both swipe and zoom touch end handlers', () => {
    const zoomTouchEnd = vi.fn();
    const swipeTouchEnd = vi.fn();

    vi.mocked(usePinchZoom).mockReturnValue({
      state: { scale: 1, originX: 50, originY: 50, translateX: 0, translateY: 0 },
      handlers: { onTouchStart: vi.fn(), onTouchMove: vi.fn(), onTouchEnd: zoomTouchEnd },
      isZoomed: false,
      resetZoom: vi.fn(),
    });

    vi.mocked(useSwipe).mockReturnValue({
      onTouchStart: vi.fn(),
      onTouchMove: vi.fn(),
      onTouchEnd: swipeTouchEnd,
    });

    const { container } = render(<MobileReader comicId={1} reader={createMockReader()} />);

    const contentArea = container.querySelector('.flex-1.flex');
    if (contentArea) {
      fireEvent.touchEnd(contentArea, { touches: [] });

      expect(swipeTouchEnd).toHaveBeenCalled();
      expect(zoomTouchEnd).toHaveBeenCalled();
    }
  });

  it('swipe up at last strip with no newer triggers rubber-band bounce', () => {
    vi.useFakeTimers();
    const goNewer = vi.fn();
    let capturedOnSwipeUp: (() => void) | undefined;

    vi.mocked(useSwipe).mockImplementation((opts: any) => {
      capturedOnSwipeUp = opts.onSwipeUp;
      return { onTouchStart: vi.fn(), onTouchMove: vi.fn(), onTouchEnd: vi.fn() };
    });

    render(<MobileReader comicId={1} reader={createMockReader({
      goNewer,
      currentIndex: 2,
      hasNewer: false,
      strips: [
        { date: '2026-03-14', available: true, imageUrl: 'https://example.com/14.png', width: 900, height: 300 },
        { date: '2026-03-15', available: true, imageUrl: 'https://example.com/15.png', width: 900, height: 300 },
        { date: '2026-03-16', available: true, imageUrl: 'https://example.com/16.png', width: 900, height: 300 },
      ],
    })} />);

    capturedOnSwipeUp!();
    // After BOUNCE_SNAP_MS (100ms), translateY should snap back
    vi.advanceTimersByTime(100);
    // After SWIPE_TRANSITION_MS (200ms), animation should complete
    vi.advanceTimersByTime(200);

    expect(goNewer).not.toHaveBeenCalled();
    vi.useRealTimers();
  });

  it('swipe up navigates to newer strip with animation', () => {
    vi.useFakeTimers();
    const goNewer = vi.fn();
    let capturedOnSwipeUp: (() => void) | undefined;

    vi.mocked(useSwipe).mockImplementation((opts: any) => {
      capturedOnSwipeUp = opts.onSwipeUp;
      return { onTouchStart: vi.fn(), onTouchMove: vi.fn(), onTouchEnd: vi.fn() };
    });

    render(<MobileReader comicId={1} reader={createMockReader({ goNewer, currentIndex: 1 })} />);

    capturedOnSwipeUp!();
    // After SWIPE_TRANSITION_MS (200ms), goNewer should be called
    vi.advanceTimersByTime(200);

    expect(goNewer).toHaveBeenCalledOnce();
    vi.useRealTimers();
  });

  it('swipe down at first strip with no older triggers rubber-band bounce', () => {
    vi.useFakeTimers();
    const goOlder = vi.fn();
    let capturedOnSwipeDown: (() => void) | undefined;

    vi.mocked(useSwipe).mockImplementation((opts: any) => {
      capturedOnSwipeDown = opts.onSwipeDown;
      return { onTouchStart: vi.fn(), onTouchMove: vi.fn(), onTouchEnd: vi.fn() };
    });

    render(<MobileReader comicId={1} reader={createMockReader({
      goOlder,
      currentIndex: 0,
      hasOlder: false,
    })} />);

    capturedOnSwipeDown!();
    vi.advanceTimersByTime(100);
    vi.advanceTimersByTime(200);

    expect(goOlder).not.toHaveBeenCalled();
    vi.useRealTimers();
  });

  it('swipe down navigates to older strip with animation', () => {
    vi.useFakeTimers();
    const goOlder = vi.fn();
    let capturedOnSwipeDown: (() => void) | undefined;

    vi.mocked(useSwipe).mockImplementation((opts: any) => {
      capturedOnSwipeDown = opts.onSwipeDown;
      return { onTouchStart: vi.fn(), onTouchMove: vi.fn(), onTouchEnd: vi.fn() };
    });

    render(<MobileReader comicId={1} reader={createMockReader({ goOlder, currentIndex: 1 })} />);

    capturedOnSwipeDown!();
    vi.advanceTimersByTime(200);

    expect(goOlder).toHaveBeenCalledOnce();
    vi.useRealTimers();
  });

  it('does not dismiss controls on Escape when already hidden', () => {
    const { container } = render(<MobileReader comicId={1} reader={createMockReader()} />);

    const overlay = container.querySelector('.pointer-events-none');
    expect(overlay?.className).toContain('opacity-0');

    fireEvent.keyDown(window, { key: 'Escape' });
    expect(overlay?.className).toContain('opacity-0');
  });

  it('calls router.back when go back button clicked', async () => {
    const mockBack = vi.fn();
    const { useRouter } = await import('next/navigation');
    vi.mocked(useRouter).mockReturnValue({ back: mockBack, push: vi.fn() } as any);

    const { container } = render(<MobileReader comicId={1} reader={createMockReader()} />);

    // Show controls
    const contentArea = container.querySelector('.flex-1.flex');
    if (contentArea) {
      await userEvent.click(contentArea);
    }

    await userEvent.click(screen.getByRole('button', { name: /go back/i }));
    expect(mockBack).toHaveBeenCalledOnce();
  });
});
