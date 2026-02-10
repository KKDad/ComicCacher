import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import AuthLayout from './layout';

const mockReplace = vi.fn();
const mockAuth = {
  isAuthenticated: false,
  isLoading: false,
};

vi.mock('next/navigation', () => ({
  useRouter: () => ({
    replace: mockReplace,
  }),
}));

vi.mock('@/hooks/use-auth', () => ({
  useAuth: () => mockAuth,
}));

describe('AuthLayout', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockAuth.isAuthenticated = false;
    mockAuth.isLoading = false;
  });

  it('shows loading state while auth is loading', () => {
    mockAuth.isLoading = true;
    mockAuth.isAuthenticated = false;

    render(
      <AuthLayout>
        <div>Child Content</div>
      </AuthLayout>
    );

    expect(screen.getByText('Loading...')).toBeInTheDocument();
    expect(screen.queryByText('Child Content')).not.toBeInTheDocument();
  });

  it('returns null when authenticated', () => {
    mockAuth.isLoading = false;
    mockAuth.isAuthenticated = true;

    const { container } = render(
      <AuthLayout>
        <div>Child Content</div>
      </AuthLayout>
    );

    expect(container.firstChild).toBeNull();
    expect(screen.queryByText('Child Content')).not.toBeInTheDocument();
  });

  it('redirects to dashboard when authenticated', () => {
    mockAuth.isLoading = false;
    mockAuth.isAuthenticated = true;

    render(
      <AuthLayout>
        <div>Child Content</div>
      </AuthLayout>
    );

    expect(mockReplace).toHaveBeenCalledWith('/');
  });

  it('renders children when not authenticated', () => {
    mockAuth.isLoading = false;
    mockAuth.isAuthenticated = false;

    render(
      <AuthLayout>
        <div>Child Content</div>
      </AuthLayout>
    );

    expect(screen.getByText('Child Content')).toBeInTheDocument();
  });

  it('does not redirect when not authenticated', () => {
    mockAuth.isLoading = false;
    mockAuth.isAuthenticated = false;

    render(
      <AuthLayout>
        <div>Child Content</div>
      </AuthLayout>
    );

    expect(mockReplace).not.toHaveBeenCalled();
  });

  it('does not show loading when not loading', () => {
    mockAuth.isLoading = false;
    mockAuth.isAuthenticated = false;

    render(
      <AuthLayout>
        <div>Child Content</div>
      </AuthLayout>
    );

    expect(screen.queryByText('Loading...')).not.toBeInTheDocument();
  });

  it('wraps children in centered container', () => {
    mockAuth.isLoading = false;
    mockAuth.isAuthenticated = false;

    const { container } = render(
      <AuthLayout>
        <div data-testid="child">Child Content</div>
      </AuthLayout>
    );

    const wrapper = container.querySelector('.min-h-screen');
    expect(wrapper).toBeInTheDocument();
    expect(wrapper).toHaveClass('flex', 'items-center', 'justify-center');
  });

  it('applies max-width constraint to children container', () => {
    mockAuth.isLoading = false;
    mockAuth.isAuthenticated = false;

    const { container } = render(
      <AuthLayout>
        <div>Child Content</div>
      </AuthLayout>
    );

    const innerContainer = container.querySelector('.max-w-\\[420px\\]');
    expect(innerContainer).toBeInTheDocument();
  });
});
