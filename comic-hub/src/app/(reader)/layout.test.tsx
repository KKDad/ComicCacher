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

vi.mock('@/contexts/user-context', () => ({
  UserProvider: ({ children, user }: { children: React.ReactNode; user: unknown }) => (
    <div data-testid="user-provider" data-user={JSON.stringify(user)}>
      {children}
    </div>
  ),
}));

describe('ReaderLayout', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders children wrapped in UserProvider when session exists', async () => {
    const mockUser = createMockUser();
    vi.mocked(getSession).mockResolvedValue(mockUser);

    const { default: ReaderLayout } = await import('./layout');
    const result = await ReaderLayout({ children: <div>reader content</div> });
    render(result);

    expect(screen.getByTestId('user-provider')).toBeInTheDocument();
    expect(screen.getByText('reader content')).toBeInTheDocument();
  });

  it('redirects to /login when session is null', async () => {
    vi.mocked(getSession).mockResolvedValue(null);

    const { default: ReaderLayout } = await import('./layout');
    await ReaderLayout({ children: <div>content</div> });

    expect(redirect).toHaveBeenCalledWith('/login');
  });
});
