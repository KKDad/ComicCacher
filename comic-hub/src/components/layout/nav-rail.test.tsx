import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { NavRail } from './nav-rail';
import { usePathname } from 'next/navigation';
import { useLogout } from '@/hooks/use-auth';
import { useUser } from '@/contexts/user-context';

vi.mock('@/hooks/use-auth', () => ({
  useLogout: vi.fn(),
}));

vi.mock('@/contexts/user-context', () => ({
  useUser: vi.fn(),
}));

describe('NavRail', () => {
  const mockLogout = vi.fn();

  beforeEach(() => {
    vi.mocked(useLogout).mockReturnValue({ logout: mockLogout, isLoggingOut: false });
    vi.mocked(usePathname).mockReturnValue('/');
    vi.mocked(useUser).mockReturnValue(null);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders base nav links for unauthenticated user', () => {
    render(<NavRail />);
    const links = screen.getAllByRole('link');
    expect(links).toHaveLength(4);
  });

  it('renders base nav buttons plus logout', () => {
    render(<NavRail />);
    // 4 base nav buttons + 1 logout button
    const buttons = screen.getAllByRole('button');
    expect(buttons).toHaveLength(5);
  });

  it('renders operations items for OPERATOR role', () => {
    vi.mocked(useUser).mockReturnValue({ username: 'operator', email: 'o@test.com', displayName: 'Operator', roles: ['USER', 'OPERATOR'], created: '2026-01-01' });
    render(<NavRail />);
    const links = screen.getAllByRole('link');
    // 4 base + 3 operations
    expect(links).toHaveLength(7);
  });

  it('renders operations items for ADMIN role (hierarchy)', () => {
    vi.mocked(useUser).mockReturnValue({ username: 'admin', email: 'a@test.com', displayName: 'Admin', roles: ['USER', 'ADMIN'], created: '2026-01-01' });
    render(<NavRail />);
    const links = screen.getAllByRole('link');
    expect(links).toHaveLength(7);
  });

  it('does not render operations items for USER role', () => {
    vi.mocked(useUser).mockReturnValue({ username: 'user', email: 'u@test.com', displayName: 'User', roles: ['USER'], created: '2026-01-01' });
    render(<NavRail />);
    const links = screen.getAllByRole('link');
    expect(links).toHaveLength(4);
  });

  it('calls logout when logout button is clicked', async () => {
    render(<NavRail />);
    const buttons = screen.getAllByRole('button');
    const logoutButton = buttons[buttons.length - 1];
    await userEvent.click(logoutButton);
    expect(mockLogout).toHaveBeenCalledOnce();
  });

  it('disables logout button when logging out', () => {
    vi.mocked(useLogout).mockReturnValue({ logout: mockLogout, isLoggingOut: true });
    render(<NavRail />);
    const buttons = screen.getAllByRole('button');
    const logoutButton = buttons[buttons.length - 1];
    expect(logoutButton).toBeDisabled();
  });
});
