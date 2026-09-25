import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Sidebar } from './sidebar';
import { usePathname } from 'next/navigation';
import { useLogout } from '@/hooks/use-auth';
import { useUser } from '@/contexts/user-context';
import { createMockUser } from '@/test/test-utils';

vi.mock('@/hooks/use-auth', () => ({
  useLogout: vi.fn(),
}));

vi.mock('@/contexts/user-context', () => ({
  useUser: vi.fn(),
}));

describe('Sidebar', () => {
  const mockLogout = vi.fn();

  beforeEach(() => {
    vi.mocked(useLogout).mockReturnValue({ logout: mockLogout, isLoggingOut: false });
    vi.mocked(usePathname).mockReturnValue('/');
    vi.mocked(useUser).mockReturnValue(null);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders base nav items as links', () => {
    render(<Sidebar />);
    expect(screen.getByRole('link', { name: 'Dashboard' })).toHaveAttribute('href', '/');
    expect(screen.getByRole('link', { name: 'Daily Reader' })).toHaveAttribute('href', '/read');
    expect(screen.getByRole('link', { name: 'Comics List' })).toHaveAttribute('href', '/comics');
    expect(screen.getByRole('link', { name: 'Preferences' })).toHaveAttribute('href', '/preferences');
  });

  it('does not link to the non-existent /api page', () => {
    render(<Sidebar />);
    expect(screen.queryByRole('link', { name: 'API' })).not.toBeInTheDocument();
  });

  it('does not nest buttons inside links', () => {
    const { container } = render(<Sidebar />);
    expect(container.querySelector('a button, button a')).toBeNull();
  });

  it('does not show operations section for USER role', () => {
    vi.mocked(useUser).mockReturnValue(createMockUser({ roles: ['USER'] }));
    render(<Sidebar />);
    expect(screen.queryByText('Operations')).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Metrics' })).not.toBeInTheDocument();
  });

  it('shows operations section for OPERATOR role', () => {
    vi.mocked(useUser).mockReturnValue(createMockUser({ roles: ['OPERATOR'] }));
    render(<Sidebar />);
    expect(screen.getByText('Operations')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Metrics' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Retrieval Status' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Batch Jobs' })).toBeInTheDocument();
  });

  it('shows operations section for ADMIN role', () => {
    vi.mocked(useUser).mockReturnValue(createMockUser({ roles: ['ADMIN'] }));
    render(<Sidebar />);
    expect(screen.getByText('Operations')).toBeInTheDocument();
  });

  it('marks the current page with aria-current', () => {
    vi.mocked(usePathname).mockReturnValue('/comics');
    render(<Sidebar />);
    expect(screen.getByRole('link', { name: 'Comics List' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: 'Dashboard' })).not.toHaveAttribute('aria-current');
  });

  it('keeps a section active on nested routes', () => {
    vi.mocked(usePathname).mockReturnValue('/comics/42');
    render(<Sidebar />);
    expect(screen.getByRole('link', { name: 'Comics List' })).toHaveAttribute('aria-current', 'page');
  });

  it('highlights active operations nav item', () => {
    vi.mocked(useUser).mockReturnValue(createMockUser({ roles: ['OPERATOR'] }));
    vi.mocked(usePathname).mockReturnValue('/metrics');
    render(<Sidebar />);
    const link = screen.getByRole('link', { name: 'Metrics' });
    expect(link).toHaveAttribute('aria-current', 'page');
    expect(link.className).toContain('bg-primary-subtle');
  });

  it('calls logout when sign out is clicked', async () => {
    render(<Sidebar />);
    await userEvent.click(screen.getByRole('button', { name: 'Sign out' }));
    expect(mockLogout).toHaveBeenCalledOnce();
  });

  it('shows signing out state', () => {
    vi.mocked(useLogout).mockReturnValue({ logout: mockLogout, isLoggingOut: true });
    render(<Sidebar />);
    expect(screen.getByRole('button', { name: 'Signing out...' })).toBeDisabled();
  });
});
