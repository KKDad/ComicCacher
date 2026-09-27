import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ReaderHeader } from './reader-header';
import { useRouter } from 'next/navigation';

const mockBack = vi.fn();
const mockGoBack = vi.hoisted(() => vi.fn());
vi.mock('@/lib/navigation-history', () => ({
  useGoBack: (fallback: string) => () => mockGoBack(fallback),
}));

describe('ReaderHeader', () => {
  const defaultProps = {
    comicName: 'Garfield',
    onFirst: vi.fn(),
    onLast: vi.fn(),
    onRandom: vi.fn(),
    isLoadingRandom: false,
  };

  beforeEach(() => {
    vi.mocked(useRouter).mockReturnValue({
      push: vi.fn(),
      replace: vi.fn(),
      prefetch: vi.fn(),
      back: mockBack,
      refresh: vi.fn(),
      forward: vi.fn(),
      bfcacheId: 'test-bfcache-id',
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

  it('goes back, falling back to the comics list, when back button clicked', async () => {
    render(<ReaderHeader {...defaultProps} />);

    await userEvent.click(screen.getByRole('button', { name: /go back/i }));
    expect(mockGoBack).toHaveBeenCalledWith('/comics');
  });

  it('renders prev/next and a favorite button when given', async () => {
    const onOlder = vi.fn();
    const onNewer = vi.fn();
    render(
      <ReaderHeader
        {...defaultProps}
        onOlder={onOlder}
        onNewer={onNewer}
        canGoNewer={false}
        favoriteButton={<button type="button">fav</button>}
      />,
    );

    await userEvent.click(screen.getByRole('button', { name: 'Previous strip' }));
    expect(onOlder).toHaveBeenCalledOnce();
    expect(screen.getByRole('button', { name: 'Next strip' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'fav' })).toBeInTheDocument();
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
