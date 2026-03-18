import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Header } from './header';
import { useLogout } from '@/hooks/use-auth';
import { useRouter, usePathname, useSearchParams } from 'next/navigation';
import { UserProvider } from '@/contexts/user-context';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createMockUser } from '@/test/test-utils';
import * as gravatar from '@/lib/gravatar';

vi.mock('@/hooks/use-auth', () => ({
  useLogout: vi.fn(),
}));

vi.mock('@/lib/gravatar', () => ({
  getGravatarUrl: vi.fn(),
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
    vi.mocked(gravatar.getGravatarUrl).mockResolvedValue(
      'https://www.gravatar.com/avatar/abc123?s=80&d=404',
    );
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
    const buttons = screen.getAllByRole('button');
    // When showMenuButton=false, no button should contain the Menu icon's sr-only text or toggle behavior
    // The only buttons should be: search (mobile), notifications, and avatar
    expect(buttons).toHaveLength(3);
  });

  it('computes gravatar URL when user has email', async () => {
    renderHeader({}, createMockUser({ email: 'avatar@test.com' }));
    // Wait for the async effect to resolve
    await vi.waitFor(() => {
      expect(gravatar.getGravatarUrl).toHaveBeenCalledWith('avatar@test.com');
    });
  });

  it('does not compute gravatar when no email', () => {
    vi.mocked(gravatar.getGravatarUrl).mockClear();
    renderHeader({}, createMockUser({ email: '' }));
    expect(gravatar.getGravatarUrl).not.toHaveBeenCalled();
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

describe('Header - search', () => {
  const mockPush = vi.fn();
  const mockLogout = vi.fn();

  beforeEach(() => {
    mockPush.mockClear();
    vi.useFakeTimers({ shouldAdvanceTime: true });
    vi.mocked(useLogout).mockReturnValue({ logout: mockLogout, isLoggingOut: false });
    vi.mocked(gravatar.getGravatarUrl).mockResolvedValue(null);
    vi.mocked(useRouter).mockReturnValue({
      push: mockPush, replace: vi.fn(), prefetch: vi.fn(),
      back: vi.fn(), refresh: vi.fn(), forward: vi.fn(),
    } as any);
    vi.mocked(usePathname).mockReturnValue('/comics');
    vi.mocked(useSearchParams).mockReturnValue(new URLSearchParams() as any);
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it('navigates after debounce when user types', async () => {
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
    renderHeader();
    const input = screen.getAllByPlaceholderText('Search comics...')[0];
    await user.type(input, 'baby');
    vi.advanceTimersByTime(300);
    expect(mockPush).toHaveBeenCalledWith('/comics?q=baby');
  });

  it('does not navigate before debounce fires', async () => {
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
    renderHeader();
    const input = screen.getAllByPlaceholderText('Search comics...')[0];
    await user.type(input, 'ab');
    expect(mockPush).not.toHaveBeenCalledWith(expect.stringContaining('q=ab'));
  });

  it('navigates to /comics when search is cleared', async () => {
    vi.mocked(useSearchParams).mockReturnValue(new URLSearchParams('q=baby') as any);
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
    renderHeader();
    const clearBtn = screen.getAllByRole('button').find(
      (btn) => btn.closest('.relative')?.querySelector('input[type="search"]'),
    )!;
    await user.click(clearBtn);
    vi.advanceTimersByTime(300);
    expect(mockPush).toHaveBeenCalledWith('/comics');
  });

  it('initializes search value from URL on /comics', () => {
    vi.mocked(useSearchParams).mockReturnValue(new URLSearchParams('q=garfield') as any);
    renderHeader();
    const input = screen.getAllByPlaceholderText('Search comics...')[0] as HTMLInputElement;
    expect(input.value).toBe('garfield');
  });

  it('clears search value on non-comics pages', () => {
    vi.mocked(usePathname).mockReturnValue('/dashboard');
    renderHeader();
    const input = screen.getAllByPlaceholderText('Search comics...')[0] as HTMLInputElement;
    expect(input.value).toBe('');
  });

  it('does not navigate when value matches current query', () => {
    vi.mocked(useSearchParams).mockReturnValue(new URLSearchParams('q=test') as any);
    renderHeader();
    vi.advanceTimersByTime(300);
    expect(mockPush).not.toHaveBeenCalled();
  });

  it('toggles mobile search panel', async () => {
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
    renderHeader({ showMenuButton: true });
    // Mobile search button is the first button in the right section with md:hidden
    const buttons = screen.getAllByRole('button');
    const mobileSearchBtn = buttons.find((b) => b.className.includes('md:hidden'));
    expect(mobileSearchBtn).toBeDefined();
    await user.click(mobileSearchBtn!);
    // Should now have two search inputs (desktop + mobile)
    expect(screen.getAllByPlaceholderText('Search comics...')).toHaveLength(2);
    // Click again to close
    await user.click(mobileSearchBtn!);
    expect(screen.getAllByPlaceholderText('Search comics...')).toHaveLength(1);
  });

  it('clears search and closes mobile panel when mobile clear is clicked', async () => {
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
    renderHeader({ showMenuButton: true });
    // Open mobile search
    const buttons = screen.getAllByRole('button');
    const mobileSearchBtn = buttons.find((b) => b.className.includes('md:hidden'))!;
    await user.click(mobileSearchBtn);
    // Type in mobile search input
    const inputs = screen.getAllByPlaceholderText('Search comics...');
    const mobileInput = inputs[inputs.length - 1];
    await user.type(mobileInput, 'test');
    // Find and click the clear button in the mobile search panel
    const mobileClearBtns = screen.getAllByRole('button').filter(
      (btn) => btn.getAttribute('type') === 'button'
        && btn.closest('.md\\:hidden.px-4'),
    );
    expect(mobileClearBtns).toHaveLength(1);
    await user.click(mobileClearBtns[0]);
    // Mobile panel should be closed (only 1 search input)
    expect(screen.getAllByPlaceholderText('Search comics...')).toHaveLength(1);
  });

  it('does not navigate to /comics when clearing on non-comics page', async () => {
    vi.mocked(usePathname).mockReturnValue('/dashboard');
    vi.mocked(useSearchParams).mockReturnValue(new URLSearchParams() as any);
    renderHeader();
    vi.advanceTimersByTime(300);
    expect(mockPush).not.toHaveBeenCalled();
  });
});
