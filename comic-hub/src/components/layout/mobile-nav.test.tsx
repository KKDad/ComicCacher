import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MobileNav } from './mobile-nav';
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

  it('renders the three bottom bar links with short labels', () => {
    render(<MobileNav />);
    expect(screen.getAllByRole('link')).toHaveLength(3);
    expect(screen.getByRole('link', { name: 'Home' })).toHaveAttribute('href', '/');
    expect(screen.getByRole('link', { name: 'Daily' })).toHaveAttribute('href', '/read');
    expect(screen.getByRole('link', { name: 'Comics' })).toHaveAttribute('href', '/comics');
  });

  it('marks the current page with aria-current', () => {
    vi.mocked(usePathname).mockReturnValue('/comics/3');
    render(<MobileNav />);
    expect(screen.getByRole('link', { name: 'Comics' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: 'Home' })).not.toHaveAttribute('aria-current');
  });

  it('opens the More sheet with preferences and no API link', async () => {
    render(<MobileNav />);
    await userEvent.click(screen.getByRole('button', { name: 'More' }));
    expect(screen.getByRole('link', { name: 'Preferences' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'API' })).not.toBeInTheDocument();
  });

  it('does not show operations for USER role', async () => {
    vi.mocked(useUser).mockReturnValue(createMockUser({ roles: ['USER'] }));
    render(<MobileNav />);
    await userEvent.click(screen.getByRole('button', { name: 'More' }));
    expect(screen.queryByText('Operations')).not.toBeInTheDocument();
  });

  it('shows operations for OPERATOR role', async () => {
    vi.mocked(useUser).mockReturnValue(createMockUser({ roles: ['OPERATOR'] }));
    render(<MobileNav />);
    await userEvent.click(screen.getByRole('button', { name: 'More' }));
    expect(screen.getByText('Operations')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Metrics' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Batch Jobs' })).toBeInTheDocument();
  });

  it('closes the sheet when a menu link is clicked', async () => {
    render(<MobileNav />);
    await userEvent.click(screen.getByRole('button', { name: 'More' }));
    await userEvent.click(screen.getByRole('link', { name: 'Preferences' }));
    expect(screen.queryByRole('link', { name: 'Preferences' })).not.toBeInTheDocument();
  });

  it('calls logout from the sheet', async () => {
    render(<MobileNav />);
    await userEvent.click(screen.getByRole('button', { name: 'More' }));
    await userEvent.click(screen.getByRole('button', { name: 'Sign out' }));
    expect(mockLogout).toHaveBeenCalledOnce();
  });
});
