import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import DashboardLayout from './layout';

const mockRequireAuth = {
  isLoading: false,
};

const mockResponsiveNav = {
  layout: 'desktop' as 'desktop' | 'tablet' | 'mobile',
};

vi.mock('@/hooks/use-auth', () => ({
  useRequireAuth: () => mockRequireAuth,
}));

vi.mock('@/hooks/use-responsive-nav', () => ({
  useResponsiveNav: () => mockResponsiveNav,
}));

vi.mock('@/components/layout/header', () => ({
  Header: ({ showMenuButton }: any) => (
    <div data-testid="header">
      Header {showMenuButton ? 'with menu' : 'without menu'}
    </div>
  ),
}));

vi.mock('@/components/layout/sidebar', () => ({
  Sidebar: () => <div data-testid="sidebar">Sidebar</div>,
}));

vi.mock('@/components/layout/nav-rail', () => ({
  NavRail: () => <div data-testid="nav-rail">NavRail</div>,
}));

vi.mock('@/components/layout/mobile-nav', () => ({
  MobileNav: () => <div data-testid="mobile-nav">MobileNav</div>,
}));

describe('DashboardLayout', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockRequireAuth.isLoading = false;
    mockResponsiveNav.layout = 'desktop';
  });

  it('shows loading state initially', () => {
    mockRequireAuth.isLoading = true;

    render(
      <DashboardLayout>
        <div>Child Content</div>
      </DashboardLayout>
    );

    expect(screen.getByText('Loading...')).toBeInTheDocument();
    expect(screen.queryByText('Child Content')).not.toBeInTheDocument();
  });

  it('renders children when not loading', () => {
    mockRequireAuth.isLoading = false;

    render(
      <DashboardLayout>
        <div>Child Content</div>
      </DashboardLayout>
    );

    expect(screen.getByText('Child Content')).toBeInTheDocument();
  });

  it('renders Sidebar on desktop layout', () => {
    mockResponsiveNav.layout = 'desktop';

    render(
      <DashboardLayout>
        <div>Content</div>
      </DashboardLayout>
    );

    expect(screen.getByTestId('sidebar')).toBeInTheDocument();
    expect(screen.queryByTestId('nav-rail')).not.toBeInTheDocument();
    expect(screen.queryByTestId('mobile-nav')).not.toBeInTheDocument();
  });

  it('renders NavRail on tablet layout', () => {
    mockResponsiveNav.layout = 'tablet';

    render(
      <DashboardLayout>
        <div>Content</div>
      </DashboardLayout>
    );

    expect(screen.getByTestId('nav-rail')).toBeInTheDocument();
    expect(screen.queryByTestId('sidebar')).not.toBeInTheDocument();
    expect(screen.queryByTestId('mobile-nav')).not.toBeInTheDocument();
  });

  it('renders MobileNav on mobile layout', () => {
    mockResponsiveNav.layout = 'mobile';

    render(
      <DashboardLayout>
        <div>Content</div>
      </DashboardLayout>
    );

    expect(screen.getByTestId('mobile-nav')).toBeInTheDocument();
    expect(screen.queryByTestId('sidebar')).not.toBeInTheDocument();
    expect(screen.queryByTestId('nav-rail')).not.toBeInTheDocument();
  });

  it('shows Header with menu button on mobile', () => {
    mockResponsiveNav.layout = 'mobile';

    render(
      <DashboardLayout>
        <div>Content</div>
      </DashboardLayout>
    );

    expect(screen.getByText('Header with menu')).toBeInTheDocument();
  });

  it('shows Header without menu button on desktop', () => {
    mockResponsiveNav.layout = 'desktop';

    render(
      <DashboardLayout>
        <div>Content</div>
      </DashboardLayout>
    );

    expect(screen.getByText('Header without menu')).toBeInTheDocument();
  });

  it('shows Header without menu button on tablet', () => {
    mockResponsiveNav.layout = 'tablet';

    render(
      <DashboardLayout>
        <div>Content</div>
      </DashboardLayout>
    );

    expect(screen.getByText('Header without menu')).toBeInTheDocument();
  });

  it('applies desktop layout classes to main content', () => {
    mockResponsiveNav.layout = 'desktop';

    const { container } = render(
      <DashboardLayout>
        <div>Content</div>
      </DashboardLayout>
    );

    const main = container.querySelector('main');
    expect(main?.className).toContain('pl-[var(--sidebar-width)]');
    expect(main?.className).not.toContain('pl-[var(--sidebar-collapsed)]');
    expect(main?.className).not.toContain('pb-[var(--mobile-nav-height)]');
  });

  it('applies tablet layout classes to main content', () => {
    mockResponsiveNav.layout = 'tablet';

    const { container } = render(
      <DashboardLayout>
        <div>Content</div>
      </DashboardLayout>
    );

    const main = container.querySelector('main');
    expect(main?.className).toContain('pl-[var(--sidebar-collapsed)]');
    expect(main?.className).not.toContain('pl-[var(--sidebar-width)]');
    expect(main?.className).not.toContain('pb-[var(--mobile-nav-height)]');
  });

  it('applies mobile layout classes to main content', () => {
    mockResponsiveNav.layout = 'mobile';

    const { container } = render(
      <DashboardLayout>
        <div>Content</div>
      </DashboardLayout>
    );

    const main = container.querySelector('main');
    expect(main?.className).toContain('pb-[var(--mobile-nav-height)]');
    expect(main?.className).not.toContain('pl-[var(--sidebar-width)]');
    expect(main?.className).not.toContain('pl-[var(--sidebar-collapsed)]');
  });

  it('wraps children in container with max-width', () => {
    const { container } = render(
      <DashboardLayout>
        <div data-testid="child">Content</div>
      </DashboardLayout>
    );

    const innerContainer = container.querySelector('.max-w-\\[var\\(--content-max-width\\)\\]');
    expect(innerContainer).toBeInTheDocument();
  });

  it('renders all layout elements when not loading', () => {
    mockRequireAuth.isLoading = false;
    mockResponsiveNav.layout = 'desktop';

    render(
      <DashboardLayout>
        <div>Content</div>
      </DashboardLayout>
    );

    expect(screen.getByTestId('header')).toBeInTheDocument();
    expect(screen.getByTestId('sidebar')).toBeInTheDocument();
    expect(screen.getByText('Content')).toBeInTheDocument();
  });
});
