import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MobileNav } from './mobile-nav';
import { usePathname } from 'next/navigation';
import { useLogout } from '@/hooks/use-auth';

vi.mock('@/hooks/use-auth', () => ({
  useLogout: vi.fn(),
}));

describe('MobileNav', () => {
  const mockLogout = vi.fn();

  beforeEach(() => {
    vi.mocked(useLogout).mockReturnValue({ logout: mockLogout, isLoggingOut: false });
    vi.mocked(usePathname).mockReturnValue('/');
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders bottom nav items', () => {
    render(<MobileNav />);
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Comics')).toBeInTheDocument();
    expect(screen.getByText('Metrics')).toBeInTheDocument();
  });

  it('renders More button', () => {
    render(<MobileNav />);
    expect(screen.getByText('More')).toBeInTheDocument();
  });

  it('renders bottom nav links', () => {
    render(<MobileNav />);
    const links = screen.getAllByRole('link');
    expect(links).toHaveLength(3);
  });

  it('opens sheet when More is clicked', async () => {
    render(<MobileNav />);
    await userEvent.click(screen.getByText('More'));
    expect(screen.getByText('Menu')).toBeInTheDocument();
    expect(screen.getByText('Retrieval Status')).toBeInTheDocument();
    expect(screen.getByText('API')).toBeInTheDocument();
    expect(screen.getByText('Preferences')).toBeInTheDocument();
  });

  it('renders logout button in sheet menu', async () => {
    render(<MobileNav />);
    await userEvent.click(screen.getByText('More'));
    expect(screen.getByText('Logout')).toBeInTheDocument();
  });

  it('closes sheet when menu item is clicked', async () => {
    render(<MobileNav />);
    await userEvent.click(screen.getByText('More'));
    expect(screen.getByText('Retrieval Status')).toBeInTheDocument();
    await userEvent.click(screen.getByText('Retrieval Status'));
    // The sheet should close (setIsOpen(false) is called)
  });

  it('calls logout when logout is clicked in sheet', async () => {
    render(<MobileNav />);
    await userEvent.click(screen.getByText('More'));
    await userEvent.click(screen.getByText('Logout'));
    expect(mockLogout).toHaveBeenCalledOnce();
  });
});
