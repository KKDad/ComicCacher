import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ForgotPasswordPage from './page';

describe('ForgotPasswordPage', () => {
  it('renders email field', () => {
    render(<ForgotPasswordPage />);
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
  });

  it('renders send button', () => {
    render(<ForgotPasswordPage />);
    expect(screen.getByRole('button', { name: /send reset instructions/i })).toBeInTheDocument();
  });

  it('disables submit when email is empty', () => {
    render(<ForgotPasswordPage />);
    expect(screen.getByRole('button', { name: /send reset instructions/i })).toBeDisabled();
  });

  it('has link back to sign in', () => {
    render(<ForgotPasswordPage />);
    expect(screen.getByRole('link', { name: /sign in/i })).toHaveAttribute('href', '/login');
  });

  it('shows success view after submit', async () => {
    const user = userEvent.setup();
    render(<ForgotPasswordPage />);

    await user.type(screen.getByLabelText(/email/i), 'test@example.com');

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /send reset instructions/i })).not.toBeDisabled();
    });

    await user.click(screen.getByRole('button', { name: /send reset instructions/i }));

    await waitFor(() => {
      expect(screen.getByText('Check your email')).toBeInTheDocument();
    });
  });

  it('shows "try again" button on success view', async () => {
    const user = userEvent.setup();
    render(<ForgotPasswordPage />);

    await user.type(screen.getByLabelText(/email/i), 'test@example.com');

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /send reset instructions/i })).not.toBeDisabled();
    });

    await user.click(screen.getByRole('button', { name: /send reset instructions/i }));

    await waitFor(() => {
      expect(screen.getByText('try again')).toBeInTheDocument();
    });
  });

  it('returns to form when "try again" is clicked', async () => {
    const user = userEvent.setup();
    render(<ForgotPasswordPage />);

    await user.type(screen.getByLabelText(/email/i), 'test@example.com');

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /send reset instructions/i })).not.toBeDisabled();
    });

    await user.click(screen.getByRole('button', { name: /send reset instructions/i }));

    await waitFor(() => {
      expect(screen.getByText('try again')).toBeInTheDocument();
    });

    await user.click(screen.getByText('try again'));

    await waitFor(() => {
      expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    });
  });

  it('shows "Return to sign in" link on success', async () => {
    const user = userEvent.setup();
    render(<ForgotPasswordPage />);

    await user.type(screen.getByLabelText(/email/i), 'test@example.com');

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /send reset instructions/i })).not.toBeDisabled();
    });

    await user.click(screen.getByRole('button', { name: /send reset instructions/i }));

    await waitFor(() => {
      expect(screen.getByRole('link', { name: /return to sign in/i })).toHaveAttribute('href', '/login');
    });
  });
});
