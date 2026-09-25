import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { NavRail } from './nav-rail';
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

  it('renders an accessibly named link for each base item', () => {
    render(<NavRail />);
    expect(screen.getAllByRole('link')).toHaveLength(4);
    for (const name of ['Dashboard', 'Daily Reader', 'Comics List', 'Preferences']) {
      expect(screen.getByRole('link', { name })).toBeInTheDocument();
    }
  });

  it('renders a named sign out button', () => {
    render(<NavRail />);
    expect(screen.getByRole('button', { name: 'Sign out' })).toBeInTheDocument();
  });

  it('renders operations items for OPERATOR role', () => {
    vi.mocked(useUser).mockReturnValue(createMockUser({ roles: ['OPERATOR'] }));
    render(<NavRail />);
    expect(screen.getAllByRole('link')).toHaveLength(7);
    expect(screen.getByRole('link', { name: 'Batch Jobs' })).toBeInTheDocument();
  });

  it('renders operations items for ADMIN role (hierarchy)', () => {
    vi.mocked(useUser).mockReturnValue(createMockUser({ roles: ['ADMIN'] }));
    render(<NavRail />);
    expect(screen.getAllByRole('link')).toHaveLength(7);
  });

  it('does not render operations items for USER role', () => {
    vi.mocked(useUser).mockReturnValue(createMockUser({ roles: ['USER'] }));
    render(<NavRail />);
    expect(screen.getAllByRole('link')).toHaveLength(4);
  });

  it('marks the current page with aria-current', () => {
    vi.mocked(usePathname).mockReturnValue('/read');
    render(<NavRail />);
    expect(screen.getByRole('link', { name: 'Daily Reader' })).toHaveAttribute('aria-current', 'page');
  });

  it('calls logout when sign out is clicked', async () => {
    render(<NavRail />);
    await userEvent.click(screen.getByRole('button', { name: 'Sign out' }));
    expect(mockLogout).toHaveBeenCalledOnce();
  });
});
