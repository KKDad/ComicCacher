import { render, screen, fireEvent, act } from '@testing-library/react';
import { useVirtualizer } from '@tanstack/react-virtual';
import { DesktopReader } from './desktop-reader';
import type { useReader, Strip } from '@/hooks/use-reader';

// jsdom doesn't implement scrollIntoView or IntersectionObserver
HTMLElement.prototype.scrollIntoView = vi.fn();

// The rendered indexes the virtualizer reports (overscan included); tests move them
const virtual = vi.hoisted(() => ({
  rendered: [0, 1],
  scrollToIndex: vi.fn(),
}));

vi.mock('@tanstack/react-virtual', () => ({
  useVirtualizer: vi.fn(() => ({
    scrollToIndex: virtual.scrollToIndex,
    getTotalSize: () => 1000,
    getVirtualItems: () =>
      virtual.rendered.map((index) => ({ index, start: index * 300, size: 300, key: String(index) })),
    measureElement: vi.fn(),
  })),
}));

vi.mock('@/generated/graphql', () => ({
  useGetComicsQuery: vi.fn().mockReturnValue({ data: null, isLoading: false }),
  useGetUserPreferencesQuery: vi.fn().mockReturnValue({ data: null, isLoading: false }),
}));

import { toast } from 'sonner';

vi.mock('sonner', () => ({
  toast: { info: vi.fn() },
}));

function createMockReader(overrides?: Partial<ReturnType<typeof useReader>>): ReturnType<typeof useReader> {
  return {
    strips: [
      { date: '2026-03-14', available: true, imageUrl: 'https://example.com/14.png', width: 900, height: 300 },
      { date: '2026-03-15', available: true, imageUrl: 'https://example.com/15.png', width: 900, height: 300 },
    ],
    currentIndex: 0,
    setCurrentIndex: vi.fn(),
    comicName: 'Garfield',
    oldest: '2020-01-01',
    newest: '2026-03-20',
    avatarUrl: null,
    hasOlder: true,
    hasNewer: true,
    isLoading: false,
    isFetchingOlder: false,
    isFetchingNewer: false,
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

/** `count` consecutive daily strips ending on 2026-03-31. */
function stripsEndingMarch31(count: number): Strip[] {
  return Array.from({ length: count }, (_, i) => {
    const d = new Date(Date.UTC(2026, 2, 31 - (count - 1 - i)));
    const date = d.toISOString().slice(0, 10);
    return { date, available: true, imageUrl: `https://example.com/${date}.png`, width: 900, height: 300 };
  });
}

const range = (from: number, to: number) => Array.from({ length: to - from + 1 }, (_, i) => from + i);

describe('DesktopReader', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    virtual.rendered = [0, 1];
  });

  it('renders comic name in header', () => {
    render(<DesktopReader reader={createMockReader()} />);
    expect(screen.getByText('Garfield')).toBeInTheDocument();
  });

  it('renders loading skeletons when isLoading is true', () => {
    const reader = createMockReader({ isLoading: true });
    const { container } = render(<DesktopReader reader={reader} />);

    // StripSkeleton renders when loading
    expect(container.querySelectorAll('[data-slot="skeleton"]').length).toBeGreaterThanOrEqual(1);
  });

  it('renders strip cards for each virtual item', () => {
    render(<DesktopReader reader={createMockReader()} />);

    // Should render 2 strips from the virtualizer mock
    const images = screen.getAllByRole('img');
    expect(images.length).toBe(2);
  });

  it('shows toast when goToFirst returns already', () => {
    const reader = createMockReader({ goToFirst: vi.fn().mockReturnValue('already') });
    render(<DesktopReader reader={reader} />);

    fireEvent.keyDown(window, { key: 'Home' });
    expect(toast.info).toHaveBeenCalledWith('Already at the first strip');
  });

  it('shows toast when goToLast returns already', () => {
    const reader = createMockReader({ goToLast: vi.fn().mockReturnValue('already') });
    render(<DesktopReader reader={reader} />);

    fireEvent.keyDown(window, { key: 'End' });
    expect(toast.info).toHaveBeenCalledWith('Already at the latest strip');
  });

  it('calls goToRandom on R key', () => {
    const goToRandom = vi.fn();
    render(<DesktopReader reader={createMockReader({ goToRandom })} />);

    fireEvent.keyDown(window, { key: 'r' });
    expect(goToRandom).toHaveBeenCalledOnce();
  });

  it('does not call goToRandom on Ctrl+R', () => {
    const goToRandom = vi.fn();
    render(<DesktopReader reader={createMockReader({ goToRandom })} />);

    fireEvent.keyDown(window, { key: 'r', ctrlKey: true });
    expect(goToRandom).not.toHaveBeenCalled();
  });

  it('calls window.history.back on Escape', () => {
    const backSpy = vi.spyOn(window.history, 'back').mockImplementation(() => {});
    render(<DesktopReader reader={createMockReader()} />);

    fireEvent.keyDown(window, { key: 'Escape' });
    expect(backSpy).toHaveBeenCalledOnce();
    backSpy.mockRestore();
  });

  it('renders with strips that have no dimensions (fallback aspect)', () => {
    const reader = createMockReader({
      strips: [
        { date: '2026-03-14', available: true, imageUrl: 'https://example.com/14.png', width: null, height: null },
        { date: '2026-03-15', available: true, imageUrl: 'https://example.com/15.png', width: null, height: null },
      ],
    });
    render(<DesktopReader reader={reader} />);
    expect(screen.getByText('Garfield')).toBeInTheDocument();
  });

  it('calls goToFirst on first button click in reader controls', () => {
    const goToFirst = vi.fn().mockReturnValue('scrolled');
    render(<DesktopReader reader={createMockReader({ goToFirst })} />);

    // The first button is in the ReaderControls which has role button with specific labels
    const firstBtn = screen.getByRole('button', { name: /first/i });
    fireEvent.click(firstBtn);
    expect(goToFirst).toHaveBeenCalled();
  });

  it('calls goToLast on last button click in reader controls', () => {
    const goToLast = vi.fn().mockReturnValue('scrolled');
    render(<DesktopReader reader={createMockReader({ goToLast })} />);

    const lastBtn = screen.getByRole('button', { name: /latest/i });
    fireEvent.click(lastBtn);
    expect(goToLast).toHaveBeenCalled();
  });

  it('handles metaKey+R without calling goToRandom', () => {
    const goToRandom = vi.fn();
    render(<DesktopReader reader={createMockReader({ goToRandom })} />);

    fireEvent.keyDown(window, { key: 'R', metaKey: true });
    expect(goToRandom).not.toHaveBeenCalled();
  });

  it('passes currentDate as null when currentIndex is out of bounds', () => {
    const reader = createMockReader({ currentIndex: 5 }); // out of bounds for 2 strips
    render(<DesktopReader reader={reader} />);
    // Should render without crashing — DatePickerPopover receives null
    expect(screen.getByText('Garfield')).toBeInTheDocument();
  });

  it('ignores keyboard events from input elements', () => {
    const goToRandom = vi.fn();
    render(
      <div>
        <DesktopReader reader={createMockReader({ goToRandom })} />
        <input data-testid="test-input" />
      </div>,
    );

    const input = screen.getByTestId('test-input');
    fireEvent.keyDown(input, { key: 'r' });
    expect(goToRandom).not.toHaveBeenCalled();
  });

  describe('virtualizer setup', () => {
    it('keys strips by date and anchors the view when strips are added', () => {
      const reader = createMockReader();
      render(<DesktopReader reader={reader} />);

      const options = vi.mocked(useVirtualizer).mock.calls.at(-1)![0];
      expect(options.anchorTo).toBe('end');
      expect(options.getItemKey!(1)).toBe('2026-03-15');
    });
  });

  describe('scrolling to the current strip', () => {
    it('jumps to the current strip on first render', () => {
      render(<DesktopReader reader={createMockReader({ currentIndex: 1 })} />);

      expect(virtual.scrollToIndex).toHaveBeenCalledOnce();
      expect(virtual.scrollToIndex).toHaveBeenCalledWith(1, { align: 'center', behavior: 'auto' });
    });

    it('does not scroll when older strips shift the current index', () => {
      const strips = stripsEndingMarch31(21);
      const { rerender } = render(
        <DesktopReader reader={createMockReader({ strips: strips.slice(10), currentIndex: 5 })} />,
      );
      virtual.scrollToIndex.mockClear();

      // Ten older strips prepended; the reader is on the same strip at a new index
      rerender(<DesktopReader reader={createMockReader({ strips, currentIndex: 15 })} />);

      expect(virtual.scrollToIndex).not.toHaveBeenCalled();
    });

    it('smooth-scrolls when the reader moves to a different strip', () => {
      const strips = stripsEndingMarch31(21);
      const { rerender } = render(<DesktopReader reader={createMockReader({ strips, currentIndex: 20 })} />);
      virtual.scrollToIndex.mockClear();

      rerender(<DesktopReader reader={createMockReader({ strips, currentIndex: 0 })} />);

      expect(virtual.scrollToIndex).toHaveBeenCalledWith(0, { align: 'center', behavior: 'smooth' });
    });

    it('records a scrolled-to strip without scrolling back to it', () => {
      vi.useFakeTimers();
      try {
        const strips = stripsEndingMarch31(21);
        const setCurrentIndex = vi.fn();
        virtual.rendered = range(8, 12);
        const { container, rerender } = render(
          <DesktopReader reader={createMockReader({ strips, currentIndex: 10, setCurrentIndex })} />,
        );
        virtual.scrollToIndex.mockClear();
        const scrollEl = container.firstChild as HTMLElement;
        Object.defineProperty(scrollEl, 'clientHeight', { value: 600 });

        scrollEl.scrollTop = 11 * 300 + 150 - 300; // centre of strip 11
        fireEvent.scroll(scrollEl);
        act(() => vi.advanceTimersByTime(150));
        expect(setCurrentIndex).toHaveBeenCalledWith(11);

        rerender(<DesktopReader reader={createMockReader({ strips, currentIndex: 11, setCurrentIndex })} />);
        expect(virtual.scrollToIndex).not.toHaveBeenCalled();
      } finally {
        vi.useRealTimers();
      }
    });
  });

  describe('infinite scroll', () => {
    const strips = stripsEndingMarch31(21);

    it('loads older strips when the first strip is rendered', () => {
      const reader = createMockReader({ strips, currentIndex: 2 });
      virtual.rendered = range(0, 5);

      render(<DesktopReader reader={reader} />);

      expect(reader.loadOlder).toHaveBeenCalledOnce();
      expect(reader.loadNewer).not.toHaveBeenCalled();
    });

    it('loads newer strips when the last strip is rendered', () => {
      const reader = createMockReader({ strips, currentIndex: 18 });
      virtual.rendered = range(15, 20);

      render(<DesktopReader reader={reader} />);

      expect(reader.loadNewer).toHaveBeenCalledOnce();
      expect(reader.loadOlder).not.toHaveBeenCalled();
    });

    it('loads one direction at a time when both ends are rendered', () => {
      const reader = createMockReader({ strips: strips.slice(0, 3), currentIndex: 1 });
      virtual.rendered = [0, 1, 2];

      render(<DesktopReader reader={reader} />);

      expect(reader.loadOlder).toHaveBeenCalledOnce();
      expect(reader.loadNewer).not.toHaveBeenCalled();
    });

    it('does not load while a page is being fetched', () => {
      const older = createMockReader({ strips: strips.slice(0, 3), isFetchingOlder: true });
      const newer = createMockReader({ strips: strips.slice(0, 3), isFetchingNewer: true });
      virtual.rendered = [0, 1, 2];

      render(<DesktopReader reader={older} />);
      render(<DesktopReader reader={newer} />);

      for (const reader of [older, newer]) {
        expect(reader.loadOlder).not.toHaveBeenCalled();
        expect(reader.loadNewer).not.toHaveBeenCalled();
      }
    });

    it('does not load at the ends of the archive', () => {
      const reader = createMockReader({ strips: strips.slice(0, 3), hasOlder: false, hasNewer: false });
      virtual.rendered = [0, 1, 2];

      render(<DesktopReader reader={reader} />);

      expect(reader.loadOlder).not.toHaveBeenCalled();
      expect(reader.loadNewer).not.toHaveBeenCalled();
    });

    it('does not load when the rendered strips are away from both ends', () => {
      const reader = createMockReader({ strips, currentIndex: 10 });
      virtual.rendered = range(7, 13);

      const { rerender } = render(<DesktopReader reader={reader} />);
      rerender(<DesktopReader reader={reader} />);
      rerender(<DesktopReader reader={reader} />);

      expect(reader.loadOlder).not.toHaveBeenCalled();
      expect(reader.loadNewer).not.toHaveBeenCalled();
    });

    it('waits for the view to reach the current strip before loading', () => {
      // Opening on the newest strip: the first render is laid out from the top
      const reader = createMockReader({ strips, currentIndex: 20, hasNewer: false });
      virtual.rendered = range(0, 5);
      const { rerender } = render(<DesktopReader reader={reader} />);

      expect(reader.loadOlder).not.toHaveBeenCalled();

      // The scroll to the current strip lands
      virtual.rendered = range(16, 20);
      rerender(<DesktopReader reader={reader} />);

      expect(reader.loadOlder).not.toHaveBeenCalled();
      expect(reader.loadNewer).not.toHaveBeenCalled();
    });

    it('stops loading once the anchored page lands (no runaway loop)', () => {
      const reader = createMockReader({ strips, currentIndex: 2 });
      virtual.rendered = range(0, 5);
      const { rerender } = render(<DesktopReader reader={reader} />);
      expect(reader.loadOlder).toHaveBeenCalledOnce();

      // The page is in flight: re-renders must not ask again
      rerender(<DesktopReader reader={{ ...reader, isFetchingOlder: true }} />);
      rerender(<DesktopReader reader={{ ...reader, isFetchingOlder: true }} />);

      // 20 older strips land; anchoring keeps the same strips in view at new indexes
      const more = [...stripsEndingMarch31(41).slice(0, 20), ...strips];
      virtual.rendered = range(20, 25);
      rerender(<DesktopReader reader={{ ...reader, strips: more, currentIndex: 22 }} />);

      expect(reader.loadOlder).toHaveBeenCalledOnce();
    });
  });
});
