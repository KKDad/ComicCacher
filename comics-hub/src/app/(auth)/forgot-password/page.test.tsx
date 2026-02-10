import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import ForgotPasswordPage from './page';

const mockRequestPasswordReset = vi.fn();

vi.mock('@/lib/api/auth', () => ({
  requestPasswordReset: (...args: any[]) => mockRequestPasswordReset(...args),
}));

vi.mock('@/components/auth/error-banner', () => ({
  ErrorBanner: ({ message, onDismiss }: any) => (
    <div data-testid="error-banner">
      {message}
      <button onClick={onDismiss}>Dismiss</button>
    </div>
  ),
}));

describe('ForgotPasswordPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders email form initially', () => {
    render(<ForgotPasswordPage />);

    expect(screen.getByText('Reset your password')).toBeInTheDocument();
    expect(screen.getByLabelText('Email')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /send reset instructions/i })).toBeInTheDocument();
  });

  it('renders Comics Hub title', () => {
    render(<ForgotPasswordPage />);
    expect(screen.getByText('Comics Hub')).toBeInTheDocument();
  });

  it('shows validation error for invalid email', async () => {
    render(<ForgotPasswordPage />);

    const emailInput = screen.getByLabelText('Email');
    fireEvent.change(emailInput, { target: { value: 'invalid-email' } });
    fireEvent.blur(emailInput);

    await waitFor(() => {
      expect(screen.getByText(/invalid email/i)).toBeInTheDocument();
    });
  });

  it('submit button is disabled when email is empty', () => {
    render(<ForgotPasswordPage />);

    const submitButton = screen.getByRole('button', { name: /send reset instructions/i });
    expect(submitButton).toBeDisabled();
  });

  it('submit button is enabled when valid email is entered', async () => {
    render(<ForgotPasswordPage />);

    const emailInput = screen.getByLabelText('Email');
    fireEvent.change(emailInput, { target: { value: 'test@example.com' } });

    await waitFor(() => {
      const submitButton = screen.getByRole('button', { name: /send reset instructions/i });
      expect(submitButton).not.toBeDisabled();
    });
  });

  it('shows loading state on submit', async () => {
    mockRequestPasswordReset.mockImplementation(() => new Promise(() => {}));

    render(<ForgotPasswordPage />);

    const emailInput = screen.getByLabelText('Email');
    fireEvent.change(emailInput, { target: { value: 'test@example.com' } });

    await waitFor(() => {
      const submitButton = screen.getByRole('button', { name: /send reset instructions/i });
      expect(submitButton).not.toBeDisabled();
    });

    const submitButton = screen.getByRole('button', { name: /send reset instructions/i });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText('Sending...')).toBeInTheDocument();
    });
  });

  it('shows success view after submission', async () => {
    mockRequestPasswordReset.mockResolvedValue(undefined);

    render(<ForgotPasswordPage />);

    const emailInput = screen.getByLabelText('Email');
    fireEvent.change(emailInput, { target: { value: 'test@example.com' } });

    await waitFor(() => {
      const submitButton = screen.getByRole('button', { name: /send reset instructions/i });
      expect(submitButton).not.toBeDisabled();
    });

    const submitButton = screen.getByRole('button', { name: /send reset instructions/i });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText('Check your email')).toBeInTheDocument();
      expect(screen.getByText(/we've sent password reset instructions/i)).toBeInTheDocument();
    });
  });

  it('shows error banner on API failure', async () => {
    mockRequestPasswordReset.mockRejectedValue(new Error('Failed to send email'));

    render(<ForgotPasswordPage />);

    const emailInput = screen.getByLabelText('Email');
    fireEvent.change(emailInput, { target: { value: 'test@example.com' } });

    await waitFor(() => {
      const submitButton = screen.getByRole('button', { name: /send reset instructions/i });
      expect(submitButton).not.toBeDisabled();
    });

    const submitButton = screen.getByRole('button', { name: /send reset instructions/i });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByTestId('error-banner')).toBeInTheDocument();
      expect(screen.getByText('Failed to send email')).toBeInTheDocument();
    });
  });

  it('shows generic error message for unknown errors', async () => {
    mockRequestPasswordReset.mockRejectedValue('Unknown error');

    render(<ForgotPasswordPage />);

    const emailInput = screen.getByLabelText('Email');
    fireEvent.change(emailInput, { target: { value: 'test@example.com' } });

    await waitFor(() => {
      const submitButton = screen.getByRole('button', { name: /send reset instructions/i });
      expect(submitButton).not.toBeDisabled();
    });

    const submitButton = screen.getByRole('button', { name: /send reset instructions/i });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText('Failed to send reset email')).toBeInTheDocument();
    });
  });

  it('calls requestPasswordReset with email', async () => {
    mockRequestPasswordReset.mockResolvedValue(undefined);

    render(<ForgotPasswordPage />);

    const emailInput = screen.getByLabelText('Email');
    fireEvent.change(emailInput, { target: { value: 'test@example.com' } });

    await waitFor(() => {
      const submitButton = screen.getByRole('button', { name: /send reset instructions/i });
      expect(submitButton).not.toBeDisabled();
    });

    const submitButton = screen.getByRole('button', { name: /send reset instructions/i });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(mockRequestPasswordReset).toHaveBeenCalledWith('test@example.com');
    });
  });

  it('"try again" resets to form view', async () => {
    mockRequestPasswordReset.mockResolvedValue(undefined);

    render(<ForgotPasswordPage />);

    const emailInput = screen.getByLabelText('Email');
    fireEvent.change(emailInput, { target: { value: 'test@example.com' } });

    await waitFor(() => {
      const submitButton = screen.getByRole('button', { name: /send reset instructions/i });
      expect(submitButton).not.toBeDisabled();
    });

    const submitButton = screen.getByRole('button', { name: /send reset instructions/i });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText('Check your email')).toBeInTheDocument();
    });

    const tryAgainButton = screen.getByRole('button', { name: /try again/i });
    fireEvent.click(tryAgainButton);

    expect(screen.getByText('Reset your password')).toBeInTheDocument();
    expect(screen.getByLabelText('Email')).toBeInTheDocument();
  });

  it('renders link to sign in page', () => {
    render(<ForgotPasswordPage />);

    const signInLink = screen.getByRole('link', { name: /sign in/i });
    expect(signInLink).toHaveAttribute('href', '/login');
  });

  it('renders "Return to sign in" link in success view', async () => {
    mockRequestPasswordReset.mockResolvedValue(undefined);

    render(<ForgotPasswordPage />);

    const emailInput = screen.getByLabelText('Email');
    fireEvent.change(emailInput, { target: { value: 'test@example.com' } });

    await waitFor(() => {
      const submitButton = screen.getByRole('button', { name: /send reset instructions/i });
      expect(submitButton).not.toBeDisabled();
    });

    const submitButton = screen.getByRole('button', { name: /send reset instructions/i });
    fireEvent.click(submitButton);

    await waitFor(() => {
      const returnLink = screen.getByRole('link', { name: /return to sign in/i });
      expect(returnLink).toHaveAttribute('href', '/login');
    });
  });

  it('disables input while submitting', async () => {
    mockRequestPasswordReset.mockImplementation(() => new Promise(() => {}));

    render(<ForgotPasswordPage />);

    const emailInput = screen.getByLabelText('Email') as HTMLInputElement;
    fireEvent.change(emailInput, { target: { value: 'test@example.com' } });

    await waitFor(() => {
      const submitButton = screen.getByRole('button', { name: /send reset instructions/i });
      expect(submitButton).not.toBeDisabled();
    });

    const submitButton = screen.getByRole('button', { name: /send reset instructions/i });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(emailInput.disabled).toBe(true);
    });
  });
});
