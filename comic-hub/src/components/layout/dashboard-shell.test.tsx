import { render, screen } from '@testing-library/react';
import { DashboardShell } from './dashboard-shell';
import { useResponsiveNav } from '@/hooks/use-responsive-nav';

vi.mock('@/hooks/use-responsive-nav', () => ({
  useResponsiveNav: vi.fn(),
}));

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
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders Sidebar on desktop layout', () => {
    vi.mocked(useResponsiveNav).mockReturnValue({ layout: 'desktop' });
    render(<DashboardShell>content</DashboardShell>);
    expect(screen.getByTestId('sidebar')).toBeInTheDocument();
    expect(screen.queryByTestId('nav-rail')).not.toBeInTheDocument();
    expect(screen.queryByTestId('mobile-nav')).not.toBeInTheDocument();
  });

  it('renders NavRail on tablet layout', () => {
    vi.mocked(useResponsiveNav).mockReturnValue({ layout: 'tablet' });
    render(<DashboardShell>content</DashboardShell>);
    expect(screen.getByTestId('nav-rail')).toBeInTheDocument();
    expect(screen.queryByTestId('sidebar')).not.toBeInTheDocument();
    expect(screen.queryByTestId('mobile-nav')).not.toBeInTheDocument();
  });

  it('renders MobileNav on mobile layout', () => {
    vi.mocked(useResponsiveNav).mockReturnValue({ layout: 'mobile' });
    render(<DashboardShell>content</DashboardShell>);
    expect(screen.getByTestId('mobile-nav')).toBeInTheDocument();
    expect(screen.queryByTestId('sidebar')).not.toBeInTheDocument();
    expect(screen.queryByTestId('nav-rail')).not.toBeInTheDocument();
  });

  it('renders a skip link to the main content', () => {
    vi.mocked(useResponsiveNav).mockReturnValue({ layout: 'desktop' });
    render(<DashboardShell>content</DashboardShell>);
    expect(screen.getByRole('link', { name: 'Skip to content' })).toHaveAttribute('href', '#main-content');
    expect(screen.getByRole('main')).toHaveAttribute('id', 'main-content');
  });

  it('renders children', () => {
    vi.mocked(useResponsiveNav).mockReturnValue({ layout: 'desktop' });
    render(<DashboardShell><div>child content</div></DashboardShell>);
    expect(screen.getByText('child content')).toBeInTheDocument();
  });
});
