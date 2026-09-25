import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ForgotPasswordPage from './page';

function ok() {
  return new Response(JSON.stringify({ ok: true }));
}

describe('ForgotPasswordPage', () => {
  beforeEach(() => {
    vi.spyOn(global, 'fetch').mockResolvedValue(ok());
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  async function submit(email = 'test@example.com') {
    const user = userEvent.setup();
    render(<ForgotPasswordPage />);
    await user.type(screen.getByLabelText(/email/i), email);
    await user.click(screen.getByRole('button', { name: /send reset link/i }));
    return user;
  }

  it('renders a single page heading and the email field', () => {
    render(<ForgotPasswordPage />);
    expect(screen.getByRole('heading', { level: 1, name: 'Reset your password' })).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
  });

  it('has link back to sign in', () => {
    render(<ForgotPasswordPage />);
    expect(screen.getByRole('link', { name: /sign in/i })).toHaveAttribute('href', '/login');
  });

  it('explains an empty email instead of disabling submit', async () => {
    const user = userEvent.setup();
    render(<ForgotPasswordPage />);
    await user.click(screen.getByRole('button', { name: /send reset link/i }));
    expect(await screen.findByText('Email is required')).toBeInTheDocument();
    expect(global.fetch).not.toHaveBeenCalled();
  });

  it('posts the email to /api/forgot-password', async () => {
    await submit();
    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith('/api/forgot-password', expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ email: 'test@example.com' }),
      }));
    });
  });

  it('shows a non-committal success view after the request succeeds', async () => {
    await submit();
    expect(await screen.findByRole('heading', { name: 'Check your email' })).toBeInTheDocument();
    expect(screen.getByText(/If an account uses test@example.com/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /return to sign in/i })).toHaveAttribute('href', '/login');
  });

  it('returns to the form from the success view', async () => {
    const user = await submit();
    await user.click(await screen.findByRole('button', { name: /try a different email/i }));
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
  });

  it('shows the server error when the request fails', async () => {
    vi.mocked(global.fetch).mockResolvedValue(
      new Response(JSON.stringify({ error: 'Could not send reset email. Please try again later.' }), { status: 502 }),
    );
    await submit();
    expect(await screen.findByRole('alert')).toHaveTextContent('Could not send reset email');
    expect(screen.queryByText('Check your email')).not.toBeInTheDocument();
  });

  it('falls back to a generic error when the server gives none', async () => {
    vi.mocked(global.fetch).mockResolvedValue(new Response(JSON.stringify({}), { status: 500 }));
    await submit();
    expect(await screen.findByRole('alert')).toHaveTextContent('Could not send reset email');
  });

  it('shows a network error when fetch throws', async () => {
    vi.mocked(global.fetch).mockRejectedValue(new Error('offline'));
    await submit();
    expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong');
  });

  it('dismisses the error banner', async () => {
    vi.mocked(global.fetch).mockRejectedValue(new Error('offline'));
    const user = await submit();
    await user.click(await screen.findByRole('button', { name: /dismiss error/i }));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
