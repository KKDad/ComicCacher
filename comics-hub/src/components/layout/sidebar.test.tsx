import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { Sidebar } from './sidebar';

const mockLogout = vi.fn();
let mockPathname = '/';

vi.mock('next/navigation', () => ({
  usePathname: () => mockPathname,
}));

vi.mock('@/hooks/use-auth', () => ({
  useAuth: () => ({
    logout: mockLogout,
  }),
}));

describe('Sidebar', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders all navigation links', () => {
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

  it('highlights Dashboard as active when on root path', () => {
    mockPathname = '/';
    render(<Sidebar />);

    const dashboardButton = screen.getByText('Dashboard').closest('button');
    expect(dashboardButton).toHaveClass('bg-primary-subtle', 'text-primary', 'font-medium');
  });

  it('highlights Comics List as active when on /comics', () => {
    mockPathname = '/comics';
    render(<Sidebar />);

    const comicsButton = screen.getByText('Comics List').closest('button');
    expect(comicsButton).toHaveClass('bg-primary-subtle', 'text-primary', 'font-medium');
  });

  it('highlights Metrics as active when on /metrics', () => {
    mockPathname = '/metrics';
    render(<Sidebar />);

    const metricsButton = screen.getByText('Metrics').closest('button');
    expect(metricsButton).toHaveClass('bg-primary-subtle', 'text-primary', 'font-medium');
  });

  it('has correct href for each navigation link', () => {
    render(<Sidebar />);

    expect(screen.getByText('Dashboard').closest('a')).toHaveAttribute('href', '/');
    expect(screen.getByText('Comics List').closest('a')).toHaveAttribute('href', '/comics');
    expect(screen.getByText('Metrics').closest('a')).toHaveAttribute('href', '/metrics');
    expect(screen.getByText('Retrieval Status').closest('a')).toHaveAttribute('href', '/retrieval-status');
    expect(screen.getByText('API').closest('a')).toHaveAttribute('href', '/api');
    expect(screen.getByText('Preferences').closest('a')).toHaveAttribute('href', '/preferences');
  });

  it('calls logout function when logout button is clicked', async () => {
    render(<Sidebar />);

    const logoutButton = screen.getByText('Logout');
    fireEvent.click(logoutButton);

    expect(mockLogout).toHaveBeenCalledTimes(1);
  });

  it('renders icons for all nav items', () => {
    const { container } = render(<Sidebar />);
    const icons = container.querySelectorAll('svg');

    // 6 nav items + 1 logout button = 7 icons
    expect(icons.length).toBe(7);
  });

  it('applies ghost variant to non-active items', () => {
    mockPathname = '/';
    render(<Sidebar />);

    const comicsButton = screen.getByText('Comics List').closest('button');
    expect(comicsButton).not.toHaveClass('bg-primary-subtle', 'text-primary', 'font-medium');
  });
});
