import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Sidebar } from './sidebar';
import { usePathname } from 'next/navigation';
import { useLogout } from '@/hooks/use-auth';
import { useUser } from '@/contexts/user-context';

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

  it('renders base nav items for all users', () => {
    render(<Sidebar />);
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Comics List')).toBeInTheDocument();
    expect(screen.getByText('API')).toBeInTheDocument();
    expect(screen.getByText('Preferences')).toBeInTheDocument();
  });

  it('does not show operations section for USER role', () => {
    vi.mocked(useUser).mockReturnValue({ username: 'user', email: 'u@test.com', displayName: 'User', roles: ['USER'], created: '2026-01-01' });
    render(<Sidebar />);
    expect(screen.queryByText('Operations')).not.toBeInTheDocument();
    expect(screen.queryByText('Metrics')).not.toBeInTheDocument();
    expect(screen.queryByText('Retrieval Status')).not.toBeInTheDocument();
    expect(screen.queryByText('Batch Jobs')).not.toBeInTheDocument();
  });

  it('shows operations section for OPERATOR role', () => {
    vi.mocked(useUser).mockReturnValue({ username: 'operator', email: 'o@test.com', displayName: 'Operator', roles: ['USER', 'OPERATOR'], created: '2026-01-01' });
    render(<Sidebar />);
    expect(screen.getByText('Operations')).toBeInTheDocument();
    expect(screen.getByText('Metrics')).toBeInTheDocument();
    expect(screen.getByText('Retrieval Status')).toBeInTheDocument();
    expect(screen.getByText('Batch Jobs')).toBeInTheDocument();
  });

  it('shows operations section for ADMIN role (hierarchy)', () => {
    vi.mocked(useUser).mockReturnValue({ username: 'admin', email: 'a@test.com', displayName: 'Admin', roles: ['USER', 'ADMIN'], created: '2026-01-01' });
    render(<Sidebar />);
    expect(screen.getByText('Operations')).toBeInTheDocument();
    expect(screen.getByText('Metrics')).toBeInTheDocument();
    expect(screen.getByText('Batch Jobs')).toBeInTheDocument();
  });

  it('renders logout button', () => {
    render(<Sidebar />);
    expect(screen.getByText('Logout')).toBeInTheDocument();
  });

  it('calls logout when logout button is clicked', async () => {
    render(<Sidebar />);
    await userEvent.click(screen.getByText('Logout'));
    expect(mockLogout).toHaveBeenCalledOnce();
  });

  it('disables logout button when logging out', () => {
    vi.mocked(useLogout).mockReturnValue({ logout: mockLogout, isLoggingOut: true });
    render(<Sidebar />);
    expect(screen.getByText('Signing out...')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /signing out/i })).toBeDisabled();
  });

  it('links to correct paths', () => {
    render(<Sidebar />);
    const links = screen.getAllByRole('link');
    const hrefs = links.map((l) => l.getAttribute('href'));
    expect(hrefs).toContain('/');
    expect(hrefs).toContain('/comics');
  });

  it('highlights active operations nav item', () => {
    vi.mocked(usePathname).mockReturnValue('/metrics');
    vi.mocked(useUser).mockReturnValue({ username: 'operator', email: 'o@test.com', displayName: 'Operator', roles: ['USER', 'OPERATOR'], created: '2026-01-01' });
    render(<Sidebar />);
    const metricsLink = screen.getByText('Metrics').closest('a')!;
    const button = metricsLink.querySelector('button')!;
    expect(button.className).toContain('bg-primary-subtle');
  });

  it('operations links point to correct paths', () => {
    vi.mocked(useUser).mockReturnValue({ username: 'operator', email: 'o@test.com', displayName: 'Operator', roles: ['USER', 'OPERATOR'], created: '2026-01-01' });
    render(<Sidebar />);
    const links = screen.getAllByRole('link');
    const hrefs = links.map((l) => l.getAttribute('href'));
    expect(hrefs).toContain('/metrics');
    expect(hrefs).toContain('/retrieval-status');
    expect(hrefs).toContain('/batch-jobs');
  });
});
