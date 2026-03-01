import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ErrorBanner } from './error-banner';

describe('ErrorBanner', () => {
  it('renders the error message', () => {
    render(<ErrorBanner message="Something went wrong" />);
    expect(screen.getByText('Something went wrong')).toBeInTheDocument();
  });

  it('has alert role', () => {
    render(<ErrorBanner message="Error" />);
    expect(screen.getByRole('alert')).toBeInTheDocument();
  });

  it('shows dismiss button when onDismiss is provided', () => {
    render(<ErrorBanner message="Error" onDismiss={() => {}} />);
    expect(screen.getByLabelText('Dismiss error')).toBeInTheDocument();
  });

  it('hides dismiss button when onDismiss is not provided', () => {
    render(<ErrorBanner message="Error" />);
    expect(screen.queryByLabelText('Dismiss error')).not.toBeInTheDocument();
  });

  it('calls onDismiss when dismiss button is clicked', async () => {
    const onDismiss = vi.fn();
    render(<ErrorBanner message="Error" onDismiss={onDismiss} />);
    await userEvent.click(screen.getByLabelText('Dismiss error'));
    expect(onDismiss).toHaveBeenCalledOnce();
  });

  it('applies custom className', () => {
    render(<ErrorBanner message="Error" className="custom-class" />);
    expect(screen.getByRole('alert')).toHaveClass('custom-class');
  });
});
