import { render, screen, fireEvent } from '@testing-library/react';
import { DesktopReader } from './desktop-reader';
import type { useReader } from '@/hooks/use-reader';

// jsdom doesn't implement scrollIntoView or IntersectionObserver
HTMLElement.prototype.scrollIntoView = vi.fn();

vi.mock('@tanstack/react-virtual', () => ({
  useVirtualizer: vi.fn().mockReturnValue({
    scrollToIndex: vi.fn(),
    getTotalSize: () => 1000,
    getVirtualItems: () => [
      { index: 0, start: 0, size: 300, key: '0' },
      { index: 1, start: 300, size: 300, key: '1' },
    ],
    measureElement: vi.fn(),
  }),
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

describe('DesktopReader', () => {
  beforeEach(() => {
    vi.clearAllMocks();
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
});
