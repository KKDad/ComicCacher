import { render, screen } from '@testing-library/react';
import { getSession } from '@/lib/auth/session';
import { redirect } from 'next/navigation';
import { headers } from 'next/headers';
import { createMockUser } from '@/test/test-utils';

vi.mock('@/lib/auth/session', () => ({
  getSession: vi.fn(),
}));

vi.mock('next/navigation', () => ({
  redirect: vi.fn(),
}));

vi.mock('next/headers', () => ({
  headers: vi.fn(),
}));

vi.mock('@/contexts/user-context', () => ({
  UserProvider: ({ children, user }: { children: React.ReactNode; user: unknown }) => (
    <div data-testid="user-provider" data-user={JSON.stringify(user)}>
      {children}
    </div>
  ),
}));

vi.mock('@/components/theme/preferences-sync', () => ({
  PreferencesSync: () => <div data-testid="preferences-sync" />,
}));

function requestHeaders(init: Record<string, string> = {}) {
  vi.mocked(headers).mockResolvedValue(new Headers(init) as Awaited<ReturnType<typeof headers>>);
}

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
    expect(screen.getByTestId('preferences-sync')).toBeInTheDocument();
    expect(screen.getByText('reader content')).toBeInTheDocument();
  });

  it('redirects to /login with the requested page when session is null', async () => {
    vi.mocked(getSession).mockResolvedValue(null);
    requestHeaders({ 'x-pathname': '/comics/5/read?date=2026-09-01' });

    const { default: ReaderLayout } = await import('./layout');
    await ReaderLayout({ children: <div>content</div> });

    expect(redirect).toHaveBeenCalledWith('/login?from=%2Fcomics%2F5%2Fread%3Fdate%3D2026-09-01');
  });

  it('redirects to plain /login without a requested path', async () => {
    vi.mocked(getSession).mockResolvedValue(null);
    requestHeaders();

    const { default: ReaderLayout } = await import('./layout');
    await ReaderLayout({ children: <div>content</div> });

    expect(redirect).toHaveBeenCalledWith('/login');
  });
});
