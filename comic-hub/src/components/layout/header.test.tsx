import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Header } from './header';
import { useLogout } from '@/hooks/use-auth';
import { UserProvider } from '@/contexts/user-context';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createMockUser } from '@/test/test-utils';

vi.mock('@/hooks/use-auth', () => ({
  useLogout: vi.fn(),
}));

function renderHeader(props: { showMenuButton?: boolean } = {}, user = createMockUser()) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <UserProvider user={user}>
        <Header {...props} />
      </UserProvider>
    </QueryClientProvider>,
  );
}

describe('Header', () => {
  const mockLogout = vi.fn();

  beforeEach(() => {
    vi.mocked(useLogout).mockReturnValue({ logout: mockLogout, isLoggingOut: false });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders Comics Hub title', () => {
    renderHeader();
    expect(screen.getByText('Comics Hub')).toBeInTheDocument();
  });

  it('renders search input', () => {
    renderHeader();
    expect(screen.getByPlaceholderText('Search comics...')).toBeInTheDocument();
  });

  it('renders user initials in avatar', () => {
    renderHeader({}, createMockUser({ displayName: 'John Doe' }));
    expect(screen.getByText('JD')).toBeInTheDocument();
  });

  it('shows "U" fallback when no user', () => {
    renderHeader({}, null as any);
    expect(screen.getByText('U')).toBeInTheDocument();
  });

  it('shows user info in dropdown', async () => {
    renderHeader({}, createMockUser({ displayName: 'Jane Smith', email: 'jane@example.com' }));
    // Click avatar to open dropdown
    const avatar = screen.getByText('JS');
    await userEvent.click(avatar);
    expect(screen.getByText('Jane Smith')).toBeInTheDocument();
    expect(screen.getByText('jane@example.com')).toBeInTheDocument();
  });

  it('renders menu button when showMenuButton is true', () => {
    renderHeader({ showMenuButton: true });
    // The menu button should be visible
    const buttons = screen.getAllByRole('button');
    // Find the menu button (it contains the Menu icon)
    expect(buttons.length).toBeGreaterThan(0);
  });

  it('does not render menu button by default', () => {
    renderHeader();
    // Default is showMenuButton=false, so no toggle button for sidebar
  });

  it('calls logout when Sign out is clicked', async () => {
    renderHeader();
    // Open dropdown
    const avatar = screen.getByText('TU');
    await userEvent.click(avatar);
    await userEvent.click(screen.getByText('Sign out'));
    expect(mockLogout).toHaveBeenCalledOnce();
  });
});
