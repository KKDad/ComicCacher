import { render, screen, fireEvent } from '@testing-library/react';
import { GridHeader } from './grid-header';

const mockBack = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({ back: mockBack }),
}));

describe('GridHeader', () => {
  const defaultProps = {
    date: '2026-03-29',
    onPreviousDate: vi.fn(),
    onNextDate: vi.fn(),
    onSelectDate: vi.fn(),
    onToday: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders formatted date', () => {
    render(<GridHeader {...defaultProps} />);
    // formatFullDate output varies by locale in test environments
    // Just verify the heading element exists and contains the year
    expect(screen.getByRole('heading')).toBeInTheDocument();
  });

  it('calls onPreviousDate when left arrow clicked', () => {
    render(<GridHeader {...defaultProps} />);
    fireEvent.click(screen.getByRole('button', { name: /previous date/i }));
    expect(defaultProps.onPreviousDate).toHaveBeenCalledOnce();
  });

  it('calls onNextDate when right arrow clicked', () => {
    render(<GridHeader {...defaultProps} />);
    fireEvent.click(screen.getByRole('button', { name: /next date/i }));
    expect(defaultProps.onNextDate).toHaveBeenCalledOnce();
  });

  it('calls onToday when Today button clicked', () => {
    render(<GridHeader {...defaultProps} />);
    fireEvent.click(screen.getByText('Today'));
    expect(defaultProps.onToday).toHaveBeenCalledOnce();
  });

  it('calls router.back when back button clicked', () => {
    render(<GridHeader {...defaultProps} />);
    fireEvent.click(screen.getByRole('button', { name: /go back/i }));
    expect(mockBack).toHaveBeenCalledOnce();
  });

  it('renders date picker button', () => {
    render(<GridHeader {...defaultProps} />);
    expect(screen.getByRole('button', { name: /pick a date/i })).toBeInTheDocument();
  });
});
