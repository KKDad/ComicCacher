import { render, screen } from '@testing-library/react';
import { DashboardShell } from './dashboard-shell';

vi.mock('@/components/layout/sidebar', () => ({
  Sidebar: () => <div data-testid="sidebar" />,
}));

vi.mock('@/components/layout/nav-rail', () => ({
  NavRail: () => <div data-testid="nav-rail" />,
}));

vi.mock('@/components/layout/mobile-nav', () => ({
  MobileNav: () => <div data-testid="mobile-nav" />,
}));

vi.mock('@/components/layout/header', () => ({
  Header: () => <div data-testid="header" />,
}));

describe('DashboardShell', () => {
  it('renders every navigation and lets CSS breakpoints pick one', () => {
    render(<DashboardShell>content</DashboardShell>);
    expect(screen.getByTestId('sidebar').parentElement).toHaveClass('hidden', 'lg:block');
    expect(screen.getByTestId('nav-rail').parentElement).toHaveClass('hidden', 'md:block', 'lg:hidden');
    expect(screen.getByTestId('mobile-nav').parentElement).toHaveClass('md:hidden');
  });

  it('offsets main content for each navigation in CSS', () => {
    render(<DashboardShell>content</DashboardShell>);
    const main = screen.getByRole('main');
    expect(main.className).toContain('md:pl-[var(--sidebar-collapsed)]');
    expect(main.className).toContain('lg:pl-[var(--sidebar-width)]');
    expect(main.className).toContain('md:pb-0');
  });

  it('renders the header and children', () => {
    render(<DashboardShell><div>child content</div></DashboardShell>);
    expect(screen.getByTestId('header')).toBeInTheDocument();
    expect(screen.getByText('child content')).toBeInTheDocument();
  });

  it('renders a skip link to the main content', () => {
    render(<DashboardShell>content</DashboardShell>);
    expect(screen.getByRole('link', { name: 'Skip to content' })).toHaveAttribute('href', '#main-content');
    expect(screen.getByRole('main')).toHaveAttribute('id', 'main-content');
  });
});
