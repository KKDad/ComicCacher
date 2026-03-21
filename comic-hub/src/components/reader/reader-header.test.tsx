import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ReaderHeader } from './reader-header';
import { useRouter } from 'next/navigation';

const mockBack = vi.fn();

describe('ReaderHeader', () => {
  const defaultProps = {
    comicName: 'Garfield',
    onFirst: vi.fn(),
    onLast: vi.fn(),
    onRandom: vi.fn(),
    isLoadingRandom: false,
    hasOlder: true,
    hasNewer: true,
  };

  beforeEach(() => {
    vi.mocked(useRouter).mockReturnValue({
      push: vi.fn(),
      replace: vi.fn(),
      prefetch: vi.fn(),
      back: mockBack,
      refresh: vi.fn(),
      forward: vi.fn(),
    });
    vi.clearAllMocks();
  });

  it('renders comic name', () => {
    render(<ReaderHeader {...defaultProps} />);

    expect(screen.getByText('Garfield')).toBeInTheDocument();
  });

  it('renders back button', () => {
    render(<ReaderHeader {...defaultProps} />);

    expect(screen.getByRole('button', { name: /go back/i })).toBeInTheDocument();
  });

  it('navigates back when back button clicked', async () => {
    render(<ReaderHeader {...defaultProps} />);

    await userEvent.click(screen.getByRole('button', { name: /go back/i }));
    expect(mockBack).toHaveBeenCalledOnce();
  });

  it('renders reader controls', () => {
    render(<ReaderHeader {...defaultProps} />);

    expect(screen.getByRole('button', { name: /first strip/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /random strip/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /latest strip/i })).toBeInTheDocument();
  });

  it('renders date picker slot', () => {
    render(
      <ReaderHeader {...defaultProps} datePicker={<div data-testid="date-picker" />} />,
    );

    expect(screen.getByTestId('date-picker')).toBeInTheDocument();
  });

  it('has fixed header with z-sticky', () => {
    const { container } = render(<ReaderHeader {...defaultProps} />);

    const header = container.querySelector('header');
    expect(header?.className).toContain('z-sticky');
    expect(header?.className).toContain('fixed');
  });
});
