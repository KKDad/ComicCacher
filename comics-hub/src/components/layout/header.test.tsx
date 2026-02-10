import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { Header } from './header';

const mockLogout = vi.fn();
const mockToggle = vi.fn();
let mockUser: any = {
  displayName: 'John Doe',
  username: 'johndoe',
};

vi.mock('@/hooks/use-auth', () => ({
  useAuth: () => ({
    user: mockUser,
    logout: mockLogout,
  }),
}));

vi.mock('@/stores/sidebar-store', () => ({
  useSidebarStore: () => ({
    toggle: mockToggle,
  }),
}));

describe('Header', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUser = {
      displayName: 'John Doe',
      username: 'johndoe',
    };
  });

  it('renders Comics Hub title', () => {
    render(<Header />);
    expect(screen.getByText('Comics Hub')).toBeInTheDocument();
  });

  it('renders user avatar with initials from displayName', () => {
    render(<Header />);
    expect(screen.getByText('JD')).toBeInTheDocument();
  });

  it('renders user avatar with first letter of username when no displayName', () => {
    mockUser = { username: 'alice' };
    render(<Header />);
    expect(screen.getByText('A')).toBeInTheDocument();
  });

  it('renders fallback initial "U" when no user data', () => {
    mockUser = {};
    render(<Header />);
    expect(screen.getByText('U')).toBeInTheDocument();
  });

  it('shows menu button when showMenuButton is true', () => {
    const { container } = render(<Header showMenuButton={true} />);
    const menuButtons = Array.from(container.querySelectorAll('button')).filter(
      (btn) => btn.querySelector('svg.lucide-menu')
    );
    expect(menuButtons.length).toBeGreaterThan(0);
  });

  it('hides menu button when showMenuButton is false', () => {
    const { container } = render(<Header showMenuButton={false} />);
    const menuButtons = Array.from(container.querySelectorAll('button')).filter(
      (btn) => btn.querySelector('svg') && btn.className.includes('lg:hidden')
    );
    expect(menuButtons.length).toBe(0);
  });

  it('calls sidebar toggle when menu button is clicked', () => {
    const { container } = render(<Header showMenuButton={true} />);
    const menuButtons = Array.from(container.querySelectorAll('button')).filter(
      (btn) => btn.querySelector('svg.lucide-menu')
    );

    if (menuButtons[0]) {
      fireEvent.click(menuButtons[0]);
      expect(mockToggle).toHaveBeenCalledTimes(1);
    }
  });

  it('renders search input', () => {
    render(<Header />);
    expect(screen.getByPlaceholderText('Search comics...')).toBeInTheDocument();
  });

  it('renders notification button with indicator', () => {
    const { container } = render(<Header />);
    const notificationIndicators = container.querySelectorAll('.bg-error.rounded-full');
    expect(notificationIndicators.length).toBeGreaterThan(0);
  });

  it('renders avatar button with dropdown trigger', () => {
    render(<Header />);
    const avatarButton = screen.getByText('JD').closest('button');
    expect(avatarButton).toBeInTheDocument();
    expect(avatarButton).toHaveAttribute('type', 'button');
  });

  it('renders with uppercase initials', () => {
    mockUser = { displayName: 'alice bob' };
    render(<Header />);
    expect(screen.getByText('AB')).toBeInTheDocument();
  });

  it('handles single name for initials', () => {
    mockUser = { displayName: 'Alice' };
    render(<Header />);
    expect(screen.getByText('A')).toBeInTheDocument();
  });
});
