import { render, screen } from '@testing-library/react';
import { GridReader } from './grid-reader';
import { useResponsiveNav } from '@/hooks/use-responsive-nav';
import { useGridReader } from '@/hooks/use-grid-reader';

vi.mock('@/hooks/use-responsive-nav');
vi.mock('@/hooks/use-grid-reader');
vi.mock('./desktop-grid-reader', () => ({
  DesktopGridReader: ({ reader }: { reader: unknown }) => <div data-testid="desktop-grid-reader">Desktop</div>,
}));
vi.mock('./mobile-grid-reader', () => ({
  MobileGridReader: ({ reader }: { reader: unknown }) => <div data-testid="mobile-grid-reader">Mobile</div>,
}));

const mockReader = {
  date: '2026-03-29',
  comics: [],
  isLoading: false,
  goToDate: vi.fn(),
  goToNextDate: vi.fn(),
  goToPreviousDate: vi.fn(),
  goToToday: vi.fn(),
};

describe('GridReader', () => {
  beforeEach(() => {
    vi.mocked(useGridReader).mockReturnValue(mockReader);
  });

  it('renders desktop grid reader on desktop', () => {
    vi.mocked(useResponsiveNav).mockReturnValue({ layout: 'desktop' });
    render(<GridReader />);
    expect(screen.getByTestId('desktop-grid-reader')).toBeInTheDocument();
  });

  it('renders desktop grid reader on tablet', () => {
    vi.mocked(useResponsiveNav).mockReturnValue({ layout: 'tablet' });
    render(<GridReader />);
    expect(screen.getByTestId('desktop-grid-reader')).toBeInTheDocument();
  });

  it('renders mobile grid reader on mobile', () => {
    vi.mocked(useResponsiveNav).mockReturnValue({ layout: 'mobile' });
    render(<GridReader />);
    expect(screen.getByTestId('mobile-grid-reader')).toBeInTheDocument();
  });

  it('passes initialDate to useGridReader', () => {
    vi.mocked(useResponsiveNav).mockReturnValue({ layout: 'desktop' });
    render(<GridReader initialDate="2026-01-15" />);
    expect(useGridReader).toHaveBeenCalledWith({ initialDate: '2026-01-15' });
  });
});
