import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { NavRail } from './nav-rail';
import { usePathname } from 'next/navigation';
import { useLogout } from '@/hooks/use-auth';

vi.mock('@/hooks/use-auth', () => ({
  useLogout: vi.fn(),
}));

describe('NavRail', () => {
  const mockLogout = vi.fn();

  beforeEach(() => {
    vi.mocked(useLogout).mockReturnValue({ logout: mockLogout, isLoggingOut: false });
    vi.mocked(usePathname).mockReturnValue('/');
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders all nav links', () => {
    render(<NavRail />);
    const links = screen.getAllByRole('link');
    expect(links).toHaveLength(6);
  });

  it('renders nav buttons', () => {
    render(<NavRail />);
    // 6 nav buttons + 1 logout button
    const buttons = screen.getAllByRole('button');
    expect(buttons.length).toBeGreaterThanOrEqual(7);
  });

  it('calls logout when logout button is clicked', async () => {
    render(<NavRail />);
    // Logout is the last button
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
