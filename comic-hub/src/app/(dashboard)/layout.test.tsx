import { render, screen } from '@testing-library/react';
import { getSession } from '@/lib/auth/session';
import { redirect } from 'next/navigation';
import { createMockUser } from '@/test/test-utils';

vi.mock('@/lib/auth/session', () => ({
  getSession: vi.fn(),
}));

vi.mock('next/navigation', () => ({
  redirect: vi.fn(),
}));

vi.mock('@/components/layout/dashboard-shell', () => ({
  DashboardShell: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="dashboard-shell">{children}</div>
  ),
}));

vi.mock('@/contexts/user-context', () => ({
  UserProvider: ({ children, user }: { children: React.ReactNode; user: unknown }) => (
    <div data-testid="user-provider" data-user={JSON.stringify(user)}>
      {children}
    </div>
  ),
}));

describe('DashboardLayout', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('passes session user to UserProvider and renders children', async () => {
    const mockUser = createMockUser();
    vi.mocked(getSession).mockResolvedValue(mockUser);

    // Dynamic import to get fresh module
    const { default: DashboardLayout } = await import('./layout');
    const result = await DashboardLayout({ children: <div>dashboard content</div> });
    render(result);

    expect(screen.getByTestId('user-provider')).toBeInTheDocument();
    expect(screen.getByTestId('dashboard-shell')).toBeInTheDocument();
    expect(screen.getByText('dashboard content')).toBeInTheDocument();
  });

  it('redirects to /login when session returns null', async () => {
    vi.mocked(getSession).mockResolvedValue(null);

    const { default: DashboardLayout } = await import('./layout');
    await DashboardLayout({ children: <div>content</div> });

    expect(redirect).toHaveBeenCalledWith('/login');
  });
});
