import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Sidebar } from './sidebar';
import { usePathname } from 'next/navigation';
import { useLogout } from '@/hooks/use-auth';

vi.mock('@/hooks/use-auth', () => ({
  useLogout: vi.fn(),
}));

describe('Sidebar', () => {
  const mockLogout = vi.fn();

  beforeEach(() => {
    vi.mocked(useLogout).mockReturnValue({ logout: mockLogout, isLoggingOut: false });
    vi.mocked(usePathname).mockReturnValue('/');
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders all nav items', () => {
    render(<Sidebar />);
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Comics List')).toBeInTheDocument();
    expect(screen.getByText('Metrics')).toBeInTheDocument();
    expect(screen.getByText('Retrieval Status')).toBeInTheDocument();
    expect(screen.getByText('API')).toBeInTheDocument();
    expect(screen.getByText('Preferences')).toBeInTheDocument();
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
    expect(hrefs).toContain('/metrics');
  });
});
