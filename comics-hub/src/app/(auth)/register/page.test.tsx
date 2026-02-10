import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import RegisterPage from './page';

const mockRegister = vi.fn();
const mockClearError = vi.fn();
const mockPush = vi.fn();

vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: mockPush,
  }),
}));

vi.mock('@/hooks/use-auth', () => ({
  useAuth: () => ({
    register: mockRegister,
    clearError: mockClearError,
  }),
}));

vi.mock('@/components/auth/error-banner', () => ({
  ErrorBanner: ({ message, onDismiss }: any) => (
    <div data-testid="error-banner">
      {message}
      <button onClick={onDismiss}>Dismiss</button>
    </div>
  ),
}));

vi.mock('@/components/ui/password-input', () => ({
  PasswordInput: (props: any) => <input type="password" {...props} />,
}));

describe('RegisterPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders all form fields', () => {
    render(<RegisterPage />);

    expect(screen.getByLabelText('Username')).toBeInTheDocument();
    expect(screen.getByLabelText('Email')).toBeInTheDocument();
    expect(screen.getByLabelText('Display Name')).toBeInTheDocument();
    expect(screen.getByLabelText('Password')).toBeInTheDocument();
    expect(screen.getByLabelText('Confirm Password')).toBeInTheDocument();
  });

  it('renders Comics Hub title', () => {
    render(<RegisterPage />);
    expect(screen.getByText('Comics Hub')).toBeInTheDocument();
  });

  it('renders Create an account title', () => {
    render(<RegisterPage />);
    expect(screen.getByText('Create an account')).toBeInTheDocument();
  });

  it('submit button is disabled when fields are empty', () => {
    render(<RegisterPage />);

    const submitButton = screen.getByRole('button', { name: /create account/i });
    expect(submitButton).toBeDisabled();
  });

  it('validates required fields', () => {
    render(<RegisterPage />);

    const usernameInput = screen.getByLabelText('Username');
    fireEvent.change(usernameInput, { target: { value: 'a' } });
    fireEvent.blur(usernameInput);

    const submitButton = screen.getByRole('button', { name: /create account/i });
    expect(submitButton).toBeDisabled();
  });

  it('validates password match', () => {
    render(<RegisterPage />);

    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password123' } });
    fireEvent.change(screen.getByLabelText('Confirm Password'), { target: { value: 'different' } });

    const submitButton = screen.getByRole('button', { name: /create account/i });
    expect(submitButton).toBeDisabled();
  });

  it('renders link to sign in page', () => {
    render(<RegisterPage />);

    const signInLink = screen.getByRole('link', { name: /sign in/i });
    expect(signInLink).toHaveAttribute('href', '/login');
  });

  it('calls registerUser on valid form submit', async () => {
    mockRegister.mockResolvedValue(undefined);

    render(<RegisterPage />);

    await act(async () => {
      fireEvent.change(screen.getByLabelText('Username'), { target: { value: 'testuser123' } });
      fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'test@example.com' } });
      fireEvent.change(screen.getByLabelText('Display Name'), { target: { value: 'Test User' } });
      fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'Password123!@#' } });
      fireEvent.change(screen.getByLabelText('Confirm Password'), { target: { value: 'Password123!@#' } });
    });

    await waitFor(() => {
      const submitButton = screen.getByRole('button', { name: /create account/i });
      expect(submitButton).not.toBeDisabled();
    });

    const submitButton = screen.getByRole('button', { name: /create account/i });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(mockRegister).toHaveBeenCalled();
    });
  });

  it('shows error banner on registration failure', async () => {
    mockRegister.mockRejectedValue(new Error('Username taken'));

    render(<RegisterPage />);

    await act(async () => {
      fireEvent.change(screen.getByLabelText('Username'), { target: { value: 'testuser123' } });
      fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'test@example.com' } });
      fireEvent.change(screen.getByLabelText('Display Name'), { target: { value: 'Test User' } });
      fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'Password123!@#' } });
      fireEvent.change(screen.getByLabelText('Confirm Password'), { target: { value: 'Password123!@#' } });
    });

    await waitFor(() => {
      const submitButton = screen.getByRole('button', { name: /create account/i });
      expect(submitButton).not.toBeDisabled();
    });

    const submitButton = screen.getByRole('button', { name: /create account/i });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByTestId('error-banner')).toBeInTheDocument();
    });
  });

  it('redirects to dashboard on success', async () => {
    mockRegister.mockResolvedValue(undefined);

    render(<RegisterPage />);

    await act(async () => {
      fireEvent.change(screen.getByLabelText('Username'), { target: { value: 'testuser123' } });
      fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'test@example.com' } });
      fireEvent.change(screen.getByLabelText('Display Name'), { target: { value: 'Test User' } });
      fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'Password123!@#' } });
      fireEvent.change(screen.getByLabelText('Confirm Password'), { target: { value: 'Password123!@#' } });
    });

    await waitFor(() => {
      const submitButton = screen.getByRole('button', { name: /create account/i });
      expect(submitButton).not.toBeDisabled();
    });

    const submitButton = screen.getByRole('button', { name: /create account/i });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/dashboard');
    });
  });
});
