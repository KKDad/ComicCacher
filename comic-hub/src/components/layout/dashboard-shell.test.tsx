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
  Header: ({ showMenuButton }: { showMenuButton?: boolean }) => (
    <div data-testid="header" data-menu-button={showMenuButton} />
  ),
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

  it('passes showMenuButton=true to Header on mobile', () => {
    vi.mocked(useResponsiveNav).mockReturnValue({ layout: 'mobile' });
    render(<DashboardShell>content</DashboardShell>);
    expect(screen.getByTestId('header')).toHaveAttribute('data-menu-button', 'true');
  });

  it('renders children', () => {
    vi.mocked(useResponsiveNav).mockReturnValue({ layout: 'desktop' });
    render(<DashboardShell><div>child content</div></DashboardShell>);
    expect(screen.getByText('child content')).toBeInTheDocument();
  });
});
