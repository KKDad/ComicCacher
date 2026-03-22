import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { DatePickerPopover } from './date-picker-popover';

vi.mock('sonner', () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}));

describe('DatePickerPopover', () => {
  const defaultProps = {
    oldest: '2020-01-01',
    newest: '2026-03-20',
    currentDate: '2026-03-15',
    onSelectDate: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders trigger button', () => {
    render(<DatePickerPopover {...defaultProps} />);

    expect(screen.getByRole('button', { name: /pick a date/i })).toBeInTheDocument();
  });

  it('opens calendar on click', async () => {
    render(<DatePickerPopover {...defaultProps} />);

    await userEvent.click(screen.getByRole('button', { name: /pick a date/i }));

    // Calendar should be visible (grid role is used by the calendar)
    expect(screen.getByRole('grid')).toBeInTheDocument();
  });

  it('handles null dates gracefully', () => {
    render(
      <DatePickerPopover
        oldest={null}
        newest={null}
        currentDate={null}
        onSelectDate={vi.fn()}
      />,
    );

    expect(screen.getByRole('button', { name: /pick a date/i })).toBeInTheDocument();
  });
});
