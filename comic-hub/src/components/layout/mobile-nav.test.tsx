import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MobileNav } from './mobile-nav';
import { usePathname } from 'next/navigation';
import { useLogout } from '@/hooks/use-auth';
import { useUser } from '@/contexts/user-context';

vi.mock('@/hooks/use-auth', () => ({
  useLogout: vi.fn(),
}));

vi.mock('@/contexts/user-context', () => ({
  useUser: vi.fn(),
}));

describe('MobileNav', () => {
  const mockLogout = vi.fn();

  beforeEach(() => {
    vi.mocked(useLogout).mockReturnValue({ logout: mockLogout, isLoggingOut: false });
    vi.mocked(usePathname).mockReturnValue('/');
    vi.mocked(useUser).mockReturnValue(null);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders bottom nav items', () => {
    render(<MobileNav />);
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Comics')).toBeInTheDocument();
  });

  it('does not show Metrics in bottom nav', () => {
    render(<MobileNav />);
    // Metrics is no longer in the bottom bar — it's in the operations menu
    const bottomLinks = screen.getAllByRole('link');
    expect(bottomLinks).toHaveLength(2);
  });

  it('renders More button', () => {
    render(<MobileNav />);
    expect(screen.getByText('More')).toBeInTheDocument();
  });

  it('opens sheet with base menu items', async () => {
    render(<MobileNav />);
    await userEvent.click(screen.getByText('More'));
    expect(screen.getByText('Menu')).toBeInTheDocument();
    expect(screen.getByText('API')).toBeInTheDocument();
    expect(screen.getByText('Preferences')).toBeInTheDocument();
  });

  it('does not show operations items for USER role', async () => {
    vi.mocked(useUser).mockReturnValue({ username: 'user', email: 'u@test.com', displayName: 'User', roles: ['USER'], created: '2026-01-01' });
    render(<MobileNav />);
    await userEvent.click(screen.getByText('More'));
    expect(screen.queryByText('Operations')).not.toBeInTheDocument();
    expect(screen.queryByText('Metrics')).not.toBeInTheDocument();
    expect(screen.queryByText('Retrieval Status')).not.toBeInTheDocument();
    expect(screen.queryByText('Batch Jobs')).not.toBeInTheDocument();
  });

  it('shows operations items for OPERATOR role', async () => {
    vi.mocked(useUser).mockReturnValue({ username: 'operator', email: 'o@test.com', displayName: 'Operator', roles: ['USER', 'OPERATOR'], created: '2026-01-01' });
    render(<MobileNav />);
    await userEvent.click(screen.getByText('More'));
    expect(screen.getByText('Operations')).toBeInTheDocument();
    expect(screen.getByText('Metrics')).toBeInTheDocument();
    expect(screen.getByText('Retrieval Status')).toBeInTheDocument();
    expect(screen.getByText('Batch Jobs')).toBeInTheDocument();
  });

  it('renders logout button in sheet menu', async () => {
    render(<MobileNav />);
    await userEvent.click(screen.getByText('More'));
    expect(screen.getByText('Logout')).toBeInTheDocument();
  });

  it('calls logout when logout is clicked in sheet', async () => {
    render(<MobileNav />);
    await userEvent.click(screen.getByText('More'));
    await userEvent.click(screen.getByText('Logout'));
    expect(mockLogout).toHaveBeenCalledOnce();
  });
});
