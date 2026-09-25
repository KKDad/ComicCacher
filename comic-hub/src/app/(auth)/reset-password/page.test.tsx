import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ResetPasswordPage from './page';
import { useRouter, useSearchParams } from 'next/navigation';

const mockRouter = {
  push: vi.fn(),
  replace: vi.fn(),
  prefetch: vi.fn(),
  back: vi.fn(),
  refresh: vi.fn(),
  forward: vi.fn(),
};

function withToken(token: string | null) {
  vi.mocked(useSearchParams).mockReturnValue(
    new URLSearchParams(token ? { token } : {}) as ReturnType<typeof useSearchParams>,
  );
}

describe('ResetPasswordPage', () => {
  beforeEach(() => {
    vi.mocked(useRouter).mockReturnValue(mockRouter);
    withToken('abc123');
    vi.spyOn(global, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ user: { username: 'u', displayName: 'U' } })),
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
    Object.values(mockRouter).forEach((fn) => fn.mockClear());
  });

  async function fill(password = 'NewPass123', confirm = password) {
    const user = userEvent.setup();
    render(<ResetPasswordPage />);
    await user.type(screen.getByLabelText(/^new password$/i), password);
    await user.type(screen.getByLabelText(/confirm new password/i), confirm);
    await user.click(screen.getByRole('button', { name: /save password/i }));
    return user;
  }

  it('asks for a new link when the token is missing', () => {
    withToken(null);
    render(<ResetPasswordPage />);
    expect(screen.getByRole('heading', { name: 'Reset link missing' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /request a new link/i })).toHaveAttribute('href', '/forgot-password');
  });

  it('describes the password rules on the field', () => {
    render(<ResetPasswordPage />);
    expect(screen.getByLabelText(/^new password$/i)).toHaveAccessibleDescription(/at least 8 characters/i);
  });

  it('posts the token and new password, then signs in', async () => {
    await fill();
    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith('/api/reset-password', expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ token: 'abc123', password: 'NewPass123', confirmPassword: 'NewPass123' }),
      }));
    });
    await waitFor(() => expect(mockRouter.push).toHaveBeenCalledWith('/'));
    expect(mockRouter.refresh).toHaveBeenCalled();
  });

  it('rejects mismatched passwords without calling the server', async () => {
    await fill('NewPass123', 'Different123');
    expect(await screen.findByText("Passwords don't match")).toBeInTheDocument();
    expect(global.fetch).not.toHaveBeenCalled();
  });

  it('shows the server error for an expired link', async () => {
    vi.mocked(global.fetch).mockResolvedValue(
      new Response(JSON.stringify({ error: 'This reset link is invalid or has expired. Request a new one.' }), { status: 400 }),
    );
    await fill();
    expect(await screen.findByRole('alert')).toHaveTextContent('invalid or has expired');
    expect(mockRouter.push).not.toHaveBeenCalled();
  });

  it('falls back to a generic error when the server gives none', async () => {
    vi.mocked(global.fetch).mockResolvedValue(new Response(JSON.stringify({}), { status: 500 }));
    await fill();
    expect(await screen.findByRole('alert')).toHaveTextContent('Password reset failed');
  });

  it('shows a network error when fetch throws', async () => {
    vi.mocked(global.fetch).mockRejectedValue(new Error('offline'));
    const user = await fill();
    expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong');
    await user.click(screen.getByRole('button', { name: /dismiss error/i }));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
