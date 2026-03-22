import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ReaderControls } from './reader-controls';

describe('ReaderControls', () => {
  const defaultProps = {
    onFirst: vi.fn(),
    onLast: vi.fn(),
    onRandom: vi.fn(),
    isLoadingRandom: false,
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders first, random, and last buttons', () => {
    render(<ReaderControls {...defaultProps} />);

    expect(screen.getByRole('button', { name: /first strip/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /random strip/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /latest strip/i })).toBeInTheDocument();
  });

  it('calls onFirst when first button clicked', async () => {
    render(<ReaderControls {...defaultProps} />);

    await userEvent.click(screen.getByRole('button', { name: /first strip/i }));
    expect(defaultProps.onFirst).toHaveBeenCalledOnce();
  });

  it('calls onLast when last button clicked', async () => {
    render(<ReaderControls {...defaultProps} />);

    await userEvent.click(screen.getByRole('button', { name: /latest strip/i }));
    expect(defaultProps.onLast).toHaveBeenCalledOnce();
  });

  it('calls onRandom when random button clicked', async () => {
    render(<ReaderControls {...defaultProps} />);

    await userEvent.click(screen.getByRole('button', { name: /random strip/i }));
    expect(defaultProps.onRandom).toHaveBeenCalledOnce();
  });

  it('first and last buttons are always enabled', () => {
    render(<ReaderControls {...defaultProps} />);

    expect(screen.getByRole('button', { name: /first strip/i })).toBeEnabled();
    expect(screen.getByRole('button', { name: /latest strip/i })).toBeEnabled();
  });

  it('disables random button when loading', () => {
    render(<ReaderControls {...defaultProps} isLoadingRandom />);

    expect(screen.getByRole('button', { name: /random strip/i })).toBeDisabled();
  });

  it('renders date picker slot when provided', () => {
    render(
      <ReaderControls {...defaultProps} datePicker={<div data-testid="date-picker" />} />,
    );

    expect(screen.getByTestId('date-picker')).toBeInTheDocument();
  });
});
